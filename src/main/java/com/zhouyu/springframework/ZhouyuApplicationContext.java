package com.zhouyu.springframework;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class ZhouyuApplicationContext {

    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private Map<String, Object> singletonObjects = new HashMap<>();

    public ZhouyuApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 扫描
        scan(configClass);

        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton") && !beanDefinition.isLazy()) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }

    }

    private Object createBean(String beanName, BeanDefinition beanDefinition){

        Class clazz = beanDefinition.getType();

        try {
            Object o = clazz.newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object bean = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(o, bean);
                }
            }

            if (o instanceof BeanNameAware) {
                ((BeanNameAware)o).setBeanName(beanName);
            }

            if (o instanceof ApplicationContextAware) {
                ((ApplicationContextAware)o).setApplicationContext(this);
            }

            if (clazz.isAnnotationPresent(Transactional.class)) {
                Object target = o;
                //replace cglib with bytebuddy as cglib has been buried since 2019 https://github.com/cglib/cglib
                o = new ByteBuddy().subclass(clazz).method(any()).intercept(InvocationHandlerAdapter.of(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("开启事务");
                        Object result = method.invoke(target, args);
                        System.out.println("提交事务");
                        return result;
                    }
                })).make().load(clazz.getClassLoader()).getLoaded().getDeclaredConstructor().newInstance();
            }

            return o;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private void scan(Class configClass) {
        // 获取要扫描的包路径
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan annotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = annotation.value();

            path = path.replace(".", "/");

            ClassLoader classLoader = this.getClass().getClassLoader();
            URL resource = classLoader.getResource("com/zhouyu/user");
            File file = new File(resource.getFile());

            List<File> classFile = new ArrayList<>();
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    if (f.isDirectory()) {
                        for (File f1 : f.listFiles()) {
                            if (!f1.isDirectory()) {
                                classFile.add(f1);
                            }
                        }
                    } else {
                        classFile.add(f);
                    }

                }
            }

            for (File cFile : classFile) {
                String absolutePath = cFile.getAbsolutePath();
                String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"))
                        .replace("\\", ".");

                try {
                    Class<?> clazz = classLoader.loadClass(className);

                    if (clazz.isAnnotationPresent(Component.class)) {
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setType(clazz);
                        beanDefinition.setLazy(clazz.isAnnotationPresent(Lazy.class));
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            beanDefinition.setScope(clazz.getAnnotation(Scope.class).value());
                        } else {
                            beanDefinition.setScope("singleton");
                        }

                        String beanName = clazz.getAnnotation(Component.class).value();
                        if (beanName.isEmpty()) {
                            beanName = Introspector.decapitalize(clazz.getSimpleName());
                        }

                        beanDefinitionMap.put(beanName, beanDefinition);

                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    public Object getBean(String beanName){
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        if (beanDefinition == null) {
            throw new NullPointerException();
        }

        if ("singleton".equals(beanDefinition.getScope())) {
            Object result = singletonObjects.get(beanName);
            if (result == null) {
                result = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, result);
            }
            return result;
        } else {
            return createBean(beanName, beanDefinition);
        }
    }
}

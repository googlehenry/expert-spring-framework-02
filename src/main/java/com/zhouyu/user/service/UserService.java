package com.zhouyu.user.service;

import com.zhouyu.springframework.*;

@Component
@Transactional
public class UserService implements BeanNameAware, ApplicationContextAware {

    @Autowired
    private OrderService orderService;

    private ZhouyuApplicationContext applicationContext;
    private String beanName;

    public void test(){
        System.out.println(orderService);
        System.out.println(applicationContext);
        System.out.println(beanName);
    }

    @Override
    public void setApplicationContext(ZhouyuApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}

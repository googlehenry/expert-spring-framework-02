package com.zhouyu.user;

import com.zhouyu.springframework.ZhouyuApplicationContext;
import com.zhouyu.user.service.UserService;

public class MyApplication {

    public static void main(String[] args) {
        ZhouyuApplicationContext applicationContext = new ZhouyuApplicationContext(AppConfig.class);
        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.test();
    }
}

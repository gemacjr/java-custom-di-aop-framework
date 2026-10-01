package com.example;

import com.example.aop.AOPProxy;
import com.example.app.NotificationServiceImpl;
import com.example.app.UserService;
import com.example.app.UserServiceImpl;
import com.example.di.Container;

public class Main {
    public static void main(String[] args) throws Exception {
        Container container = new Container(UserServiceImpl.class, NotificationServiceImpl.class);
        UserService userService = AOPProxy.createProxy(container.getService(UserService.class), UserService.class);
        userService.registerUser("Alice");
    }
}

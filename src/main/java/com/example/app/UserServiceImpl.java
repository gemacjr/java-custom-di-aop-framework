package com.example.app;

import com.example.aop.LogExecutionTime;
import com.example.di.Inject;
import com.example.di.Service;

@Service
public class UserServiceImpl implements UserService {
    @Inject
    private NotificationService notificationService;

    @Override
    @LogExecutionTime
    public void registerUser(String username) {
        System.out.println("User registered: " + username);
        notificationService.sendNotification(username);
    }
}

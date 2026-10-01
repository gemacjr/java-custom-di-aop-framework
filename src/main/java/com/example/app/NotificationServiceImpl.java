package com.example.app;

import com.example.di.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Override
    public void sendNotification(String username) {
        System.out.println("Notification sent to " + username);
    }
}

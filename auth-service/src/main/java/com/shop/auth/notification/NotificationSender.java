package com.shop.auth.notification;

public interface NotificationSender {
    void sendPasswordReset(String email, String resetUrl);
}

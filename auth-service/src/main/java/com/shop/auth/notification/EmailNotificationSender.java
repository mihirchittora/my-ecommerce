package com.shop.auth.notification;

import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {
    private final EmailDeliveryService delivery;

    public EmailNotificationSender(EmailDeliveryService delivery) {
        this.delivery = delivery;
    }

    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        delivery.sendPasswordReset(email, resetUrl);
    }
}

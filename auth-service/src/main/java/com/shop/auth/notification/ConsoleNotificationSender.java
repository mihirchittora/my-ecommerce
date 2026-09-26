package com.shop.auth.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationSender.class);

    @Override
    public void sendPasswordReset(String email, String resetUrl) {
        log.info("Password reset notification queued for recipient hash={}", Integer.toHexString(email.hashCode()));
    }
}

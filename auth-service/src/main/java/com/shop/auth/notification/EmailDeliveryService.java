package com.shop.auth.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final Optional<JavaMailSender> mailSender;
    private final EmailProperties properties;

    public EmailDeliveryService(ObjectProvider<JavaMailSender> mailSenders, EmailProperties properties) {
        this.mailSender = Optional.ofNullable(mailSenders.getIfAvailable());
        this.properties = properties;
    }

    public void sendWelcome(String email, String firstName) {
        String name = firstName == null || firstName.isBlank() ? "there" : firstName.trim();
        send(email, "Welcome to Morrow Commerce",
                "Hi " + name + ",\n\nYour Morrow Commerce account is ready. You can sign in at "
                        + properties.getFrontendBaseUrl() + "/login\n\nThank you,\nMorrow Commerce");
    }

    public void sendPasswordReset(String email, String resetUrl) {
        send(email, "Reset your Morrow Commerce password",
                "We received a request to reset your password.\n\nUse this link within the next 30 minutes:\n"
                        + resetUrl + "\n\nIf you did not request this, you can ignore this email.\n\nMorrow Commerce");
    }

    public void send(String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) return;
        String normalized = recipient.trim();
        if (!properties.isEnabled()) {
            log.info("Email delivery disabled; skipped subject={} recipientHash={}", subject, recipientHash(normalized));
            return;
        }
        if (mailSender.isEmpty()) {
            log.error("Email delivery enabled but no JavaMailSender is configured; subject={} recipientHash={}",
                    subject, recipientHash(normalized));
            return;
        }
        try {
            var message = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (properties.getFrom() != null && !properties.getFrom().isBlank()) {
                if (properties.getFromName() == null || properties.getFromName().isBlank()) {
                    helper.setFrom(properties.getFrom());
                } else {
                    helper.setFrom(properties.getFrom(), properties.getFromName());
                }
            }
            helper.setTo(normalized);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.get().send(message);
            log.info("Email delivered; subject={} recipientHash={}", subject, recipientHash(normalized));
        } catch (Exception ex) {
            log.error("Email delivery failed; subject={} recipientHash={}", subject, recipientHash(normalized), ex);
        }
    }

    private String recipientHash(String email) { return Integer.toHexString(email.hashCode()); }
}

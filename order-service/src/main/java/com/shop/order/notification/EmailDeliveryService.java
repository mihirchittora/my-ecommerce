package com.shop.order.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public void sendOrderCreated(String email, String orderNumber, BigDecimal totalAmount, String currency) {
        send(email, "Order " + orderNumber + " received",
                "Thank you for your order.\n\nOrder: " + orderNumber + "\nTotal: " + currency + " " + totalAmount
                        + "\n\nWe will keep you updated as your order moves through fulfillment.\n\nMorrow Commerce");
    }

    public void sendOrderCancelled(String email, String orderNumber) {
        send(email, "Order " + orderNumber + " cancelled",
                "Your order " + orderNumber + " has been cancelled.\n\nIf you need help, please contact support.\n\nMorrow Commerce");
    }

    public void sendPaymentStatus(String email, String orderNumber, String paymentId, String status,
                                  BigDecimal amount, String currency) {
        boolean refund = status != null && status.contains("REFUND");
        String amountText = refund || amount == null || currency == null ? "" : " for " + currency + " " + amount;
        send(email, "Payment update for order " + orderNumber,
                "Payment status: " + status + amountText + "\nOrder: " + orderNumber + "\nPayment reference: "
                        + paymentId + "\n\nMorrow Commerce");
    }

    public void sendShipmentStatus(String email, String orderNumber, String shipmentNumber, String status) {
        send(email, "Shipping update for order " + orderNumber,
                "Your order " + orderNumber + " has a shipping update.\n\nShipment: " + shipmentNumber
                        + "\nStatus: " + status + "\n\nMorrow Commerce");
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

package com.shop.order.service;

import com.shop.order.client.ShippingClient;
import com.shop.order.fulfillment.FulfillmentOutbox;
import com.shop.order.fulfillment.FulfillmentOutboxRepository;
import com.shop.order.fulfillment.FulfillmentOutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FulfillmentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(FulfillmentOrchestrator.class);

    private final FulfillmentOutboxRepository outbox;
    private final ShippingClient shipping;
    private final int batchSize;

    public FulfillmentOrchestrator(FulfillmentOutboxRepository outbox, ShippingClient shipping,
                                   @Value("${app.fulfillment.outbox-batch-size:20}") int batchSize) {
        this.outbox = outbox;
        this.shipping = shipping;
        this.batchSize = Math.max(1, Math.min(batchSize, 100));
    }

    @Transactional
    public void enqueue(UUID orderId) {
        if (outbox.findByOrderId(orderId).isPresent()) return;
        FulfillmentOutbox event = new FulfillmentOutbox();
        event.setOrderId(orderId);
        event.setStatus(FulfillmentOutboxStatus.PENDING);
        outbox.save(event);
    }

    @Scheduled(fixedDelayString = "${app.fulfillment.outbox-poll-ms:5000}")
    public void processPending() {
        List<FulfillmentOutbox> pending = outbox.findByStatusOrderByCreatedAtAsc(
                FulfillmentOutboxStatus.PENDING, PageRequest.of(0, batchSize));
        pending.forEach(this::deliver);
    }

    private void deliver(FulfillmentOutbox event) {
        try {
            shipping.createFulfillment(event.getOrderId());
            markCompleted(event.getId());
            log.info("Fulfillment created or confirmed for orderId={}", event.getOrderId());
        } catch (RuntimeException ex) {
            recordFailure(event.getId(), ex.getMessage());
            log.warn("Fulfillment creation will be retried for orderId={}: {}", event.getOrderId(), ex.getMessage());
        }
    }

    @Transactional
    protected void markCompleted(UUID eventId) {
        outbox.findById(eventId).ifPresent(event -> {
            event.setStatus(FulfillmentOutboxStatus.COMPLETED);
            outbox.save(event);
        });
    }

    @Transactional
    protected void recordFailure(UUID eventId, String message) {
        outbox.findById(eventId).ifPresent(event -> {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastError(message == null ? "Unknown Shipping Service error" : message.substring(0, Math.min(message.length(), 2000)));
            outbox.save(event);
        });
    }
}

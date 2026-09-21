package com.shop.order.service;

import com.shop.order.domain.OrderIdempotency;
import com.shop.order.domain.OrderIdempotencyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderIdempotencyService {
    private final OrderIdempotencyRepository repository;

    public OrderIdempotencyService(OrderIdempotencyRepository repository) {
        this.repository = repository;
    }

    public ClaimResult claim(String customerId, String idempotencyKey, String requestHash) {
        return repository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(existing -> new ClaimResult(existing, false))
                .orElseGet(() -> createOrReadRace(customerId, idempotencyKey, requestHash));
    }

    private ClaimResult createOrReadRace(String customerId, String idempotencyKey, String requestHash) {
        OrderIdempotency claim = new OrderIdempotency();
        claim.setCustomerId(customerId);
        claim.setIdempotencyKey(idempotencyKey);
        claim.setRequestHash(requestHash);
        try {
            return new ClaimResult(repository.saveAndFlush(claim), true);
        } catch (DataIntegrityViolationException ex) {
            return repository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                    .map(existing -> new ClaimResult(existing, false)).orElseThrow(() -> ex);
        }
    }

    public void linkToOrder(OrderIdempotency claim, UUID orderId) {
        claim.setOrderId(orderId);
        repository.saveAndFlush(claim);
    }

    public void delete(OrderIdempotency claim) {
        repository.deleteById(claim.getId());
    }

    public record ClaimResult(OrderIdempotency claim, boolean newlyCreated) {
    }
}

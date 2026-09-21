package com.shop.cart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CartExpirationJob {
    private static final Logger log = LoggerFactory.getLogger(CartExpirationJob.class);
    private final CartWriteService writes;

    public CartExpirationJob(CartWriteService writes) {
        this.writes = writes;
    }

    @Scheduled(fixedDelayString = "${cart.expiration.fixed-delay-ms:60000}")
    public void expireDueCarts() {
        int expired = writes.expireDueCarts();
        if (expired > 0) log.info("Expired {} active carts", expired);
    }
}

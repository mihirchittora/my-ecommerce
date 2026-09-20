package com.shop.inventory.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationJob {
    private final ReservationService reservations;

    public ReservationExpirationJob(ReservationService reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${inventory.expiration.fixed-delay-ms:60000}")
    public void expireReservations() {
        reservations.expireDueReservations();
    }
}

package com.shop.order.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReservation(
        UUID reservationId,
        String sku,
        UUID locationId,
        long quantity,
        String referenceId,
        String status,
        Instant expiresAt,
        List<ReservedUnit> units) {
    public record ReservedUnit(UUID unitId, String unitCode, String status) {
    }
}

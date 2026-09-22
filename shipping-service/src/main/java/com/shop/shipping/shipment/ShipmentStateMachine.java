package com.shop.shipping.shipment;

import com.shop.shipping.common.ConflictException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ShipmentStateMachine {
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(ShipmentStatus.CREATED, EnumSet.of(ShipmentStatus.READY, ShipmentStatus.FAILED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.READY, EnumSet.of(ShipmentStatus.PACKED, ShipmentStatus.SHIPPED, ShipmentStatus.FAILED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.PACKED, EnumSet.of(ShipmentStatus.SHIPPED, ShipmentStatus.FAILED, ShipmentStatus.CANCELLED)),
            Map.entry(ShipmentStatus.SHIPPED, EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED, ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.IN_TRANSIT, EnumSet.of(ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED, ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.OUT_FOR_DELIVERY, EnumSet.of(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED, ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.DELIVERY_FAILED, EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED, ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.DELIVERED, EnumSet.of(ShipmentStatus.RETURNED)),
            Map.entry(ShipmentStatus.RETURNED, EnumSet.noneOf(ShipmentStatus.class)),
            Map.entry(ShipmentStatus.CANCELLED, EnumSet.noneOf(ShipmentStatus.class)),
            Map.entry(ShipmentStatus.FAILED, EnumSet.of(ShipmentStatus.READY, ShipmentStatus.CANCELLED)));

    private ShipmentStateMachine() { }

    public static void requireTransition(ShipmentStatus from, ShipmentStatus to) {
        if (from != to && !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException("Shipment cannot transition from " + from + " to " + to);
        }
    }

    public static boolean cancellable(ShipmentStatus status) {
        return status == ShipmentStatus.CREATED || status == ShipmentStatus.READY
                || status == ShipmentStatus.PACKED || status == ShipmentStatus.FAILED;
    }
}

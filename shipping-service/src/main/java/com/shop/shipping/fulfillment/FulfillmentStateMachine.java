package com.shop.shipping.fulfillment;

import com.shop.shipping.common.ConflictException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class FulfillmentStateMachine {
    private static final Map<FulfillmentStatus, Set<FulfillmentStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(FulfillmentStatus.PENDING, EnumSet.of(FulfillmentStatus.READY, FulfillmentStatus.CANCELLED, FulfillmentStatus.FAILED)),
            Map.entry(FulfillmentStatus.READY, EnumSet.of(FulfillmentStatus.ALLOCATING, FulfillmentStatus.PACKED, FulfillmentStatus.CANCELLED, FulfillmentStatus.FAILED)),
            Map.entry(FulfillmentStatus.ALLOCATING, EnumSet.of(FulfillmentStatus.PACKED, FulfillmentStatus.PARTIALLY_SHIPPED, FulfillmentStatus.SHIPPED, FulfillmentStatus.FAILED, FulfillmentStatus.CANCELLED)),
            Map.entry(FulfillmentStatus.PACKED, EnumSet.of(FulfillmentStatus.PARTIALLY_SHIPPED, FulfillmentStatus.SHIPPED, FulfillmentStatus.CANCELLED, FulfillmentStatus.FAILED)),
            Map.entry(FulfillmentStatus.PARTIALLY_SHIPPED, EnumSet.of(FulfillmentStatus.PARTIALLY_SHIPPED, FulfillmentStatus.SHIPPED, FulfillmentStatus.DELIVERED, FulfillmentStatus.CANCELLED)),
            Map.entry(FulfillmentStatus.SHIPPED, EnumSet.of(FulfillmentStatus.DELIVERED, FulfillmentStatus.COMPLETED)),
            Map.entry(FulfillmentStatus.DELIVERED, EnumSet.of(FulfillmentStatus.COMPLETED)),
            Map.entry(FulfillmentStatus.COMPLETED, EnumSet.noneOf(FulfillmentStatus.class)),
            Map.entry(FulfillmentStatus.CANCELLED, EnumSet.noneOf(FulfillmentStatus.class)),
            Map.entry(FulfillmentStatus.FAILED, EnumSet.of(FulfillmentStatus.READY, FulfillmentStatus.ALLOCATING, FulfillmentStatus.CANCELLED)));

    private FulfillmentStateMachine() { }

    public static void requireTransition(FulfillmentStatus from, FulfillmentStatus to) {
        if (from != to && !TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new ConflictException("Fulfillment cannot transition from " + from + " to " + to);
        }
    }
}

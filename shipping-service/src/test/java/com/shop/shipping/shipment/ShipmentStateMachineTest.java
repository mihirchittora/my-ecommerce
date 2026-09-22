package com.shop.shipping.shipment;

import com.shop.shipping.common.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipmentStateMachineTest {
    @Test
    void acceptsCarrierAndDeliveryLifecycle() {
        assertDoesNotThrow(() -> ShipmentStateMachine.requireTransition(ShipmentStatus.READY, ShipmentStatus.SHIPPED));
        assertDoesNotThrow(() -> ShipmentStateMachine.requireTransition(ShipmentStatus.IN_TRANSIT, ShipmentStatus.DELIVERED));
    }

    @Test
    void rejectsIllegalLifecycleChange() {
        assertThrows(ConflictException.class,
                () -> ShipmentStateMachine.requireTransition(ShipmentStatus.DELIVERED, ShipmentStatus.CANCELLED));
    }
}

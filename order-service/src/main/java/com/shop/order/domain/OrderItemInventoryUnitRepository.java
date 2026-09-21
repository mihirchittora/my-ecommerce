package com.shop.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemInventoryUnitRepository extends JpaRepository<OrderItemInventoryUnit, UUID> {
    List<OrderItemInventoryUnit> findByOrderItem_IdOrderByInventoryUnitId(UUID orderItemId);
}

package com.shop.shipping.fulfillment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FulfillmentItemRepository extends JpaRepository<FulfillmentItemEntity, UUID> {
    List<FulfillmentItemEntity> findByFulfillment_IdOrderByCreatedAt(UUID fulfillmentId);
    Optional<FulfillmentItemEntity> findByFulfillment_IdAndOrderItemId(UUID fulfillmentId, UUID orderItemId);
}

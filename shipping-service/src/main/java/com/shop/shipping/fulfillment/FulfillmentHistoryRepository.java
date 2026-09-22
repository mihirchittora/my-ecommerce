package com.shop.shipping.fulfillment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FulfillmentHistoryRepository extends JpaRepository<FulfillmentHistoryEntity, UUID> {
    List<FulfillmentHistoryEntity> findByFulfillment_IdOrderByCreatedAtAsc(UUID fulfillmentId);
}

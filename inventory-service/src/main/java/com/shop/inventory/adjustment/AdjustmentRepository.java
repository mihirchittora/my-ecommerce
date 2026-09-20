package com.shop.inventory.adjustment;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {
    boolean existsByLocation_Id(UUID locationId);

    Optional<InventoryAdjustment> findFirstBySkuAndLocation_IdAndReferenceIdOrderByCreatedAtAsc(
            String sku, UUID locationId, String referenceId);

    Page<InventoryAdjustment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<InventoryAdjustment> findBySkuOrderByCreatedAtDesc(String sku, Pageable pageable);

    Page<InventoryAdjustment> findByReasonOrderByCreatedAtDesc(AdjustmentReason reason, Pageable pageable);

    Page<InventoryAdjustment> findBySkuAndReasonOrderByCreatedAtDesc(
            String sku, AdjustmentReason reason, Pageable pageable);
}

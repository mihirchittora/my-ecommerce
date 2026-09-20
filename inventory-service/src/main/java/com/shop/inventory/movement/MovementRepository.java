package com.shop.inventory.movement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<InventoryUnitMovement, UUID> {
    boolean existsByFromLocation_Id(UUID locationId);
    boolean existsByToLocation_Id(UUID locationId);

    List<InventoryUnitMovement> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    @EntityGraph(attributePaths = {"inventoryUnit", "fromLocation", "toLocation"})
    List<InventoryUnitMovement> findByReferenceTypeOrderByCreatedAtDesc(String referenceType);

    @EntityGraph(attributePaths = {"inventoryUnit", "fromLocation", "toLocation"})
    List<InventoryUnitMovement> findByReferenceTypeAndInventoryUnit_SkuOrderByCreatedAtDesc(
            String referenceType, String sku);
}

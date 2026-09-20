package com.shop.inventory.item;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    @Query("select distinct item.sku from InventoryItem item order by item.sku")
    List<String> findDistinctSkusOrderBySku();

    @EntityGraph(attributePaths = "location")
    List<InventoryItem> findBySkuOrderByLocation_Code(String sku);

    @EntityGraph(attributePaths = "location")
    Optional<InventoryItem> findBySkuAndLocation_Id(String sku, UUID locationId);

    boolean existsByLocation_Id(UUID locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item join fetch item.location where item.sku = :sku and item.location.id = :locationId")
    Optional<InventoryItem> findForUpdate(@Param("sku") String sku, @Param("locationId") UUID locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from InventoryItem item join fetch item.location where item.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") UUID id);
}

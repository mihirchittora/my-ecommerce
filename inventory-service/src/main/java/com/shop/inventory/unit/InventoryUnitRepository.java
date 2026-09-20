package com.shop.inventory.unit;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryUnitRepository extends JpaRepository<InventoryUnit, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select unit from InventoryUnit unit
            where unit.inventoryItem.sku = :sku
              and unit.inventoryItem.location.id = :locationId
              and unit.status = com.shop.inventory.unit.UnitStatus.AVAILABLE
            order by unit.createdAt, unit.id
            """)
    List<InventoryUnit> findAvailableForUpdate(
            @Param("sku") String sku,
            @Param("locationId") UUID locationId,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select unit from InventoryUnit unit join fetch unit.inventoryItem item join fetch item.location where unit.id = :id")
    Optional<InventoryUnit> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select unit from InventoryUnit unit join fetch unit.inventoryItem item join fetch item.location where unit.id in :ids")
    List<InventoryUnit> findAllByIdInForUpdate(@Param("ids") List<UUID> ids);

    @Query(value = """
            select unit.* from inventory_units unit
            join inventory_items item on item.id = unit.inventory_item_id
            where unit.sku = :sku
              and (cast(:locationId as uuid) is null or item.location_id = cast(:locationId as uuid))
              and (cast(:status as varchar) is null or unit.status = cast(:status as varchar))
              and (cast(:serialNumber as varchar) is null or lower(unit.serial_number) like lower(concat('%', cast(:serialNumber as varchar), '%')))
              and (cast(:imei as varchar) is null or lower(unit.imei) like lower(concat('%', cast(:imei as varchar), '%')))
              and (cast(:barcode as varchar) is null or lower(unit.barcode) like lower(concat('%', cast(:barcode as varchar), '%')))
            """,
            countQuery = """
            select count(*) from inventory_units unit
            join inventory_items item on item.id = unit.inventory_item_id
            where unit.sku = :sku
              and (cast(:locationId as uuid) is null or item.location_id = cast(:locationId as uuid))
              and (cast(:status as varchar) is null or unit.status = cast(:status as varchar))
              and (cast(:serialNumber as varchar) is null or lower(unit.serial_number) like lower(concat('%', cast(:serialNumber as varchar), '%')))
              and (cast(:imei as varchar) is null or lower(unit.imei) like lower(concat('%', cast(:imei as varchar), '%')))
              and (cast(:barcode as varchar) is null or lower(unit.barcode) like lower(concat('%', cast(:barcode as varchar), '%')))
            """, nativeQuery = true)
    Page<InventoryUnit> search(
            @Param("sku") String sku,
            @Param("locationId") UUID locationId,
            @Param("status") String status,
            @Param("serialNumber") String serialNumber,
            @Param("imei") String imei,
            @Param("barcode") String barcode,
            Pageable pageable);

    long countByInventoryItem_IdAndStatus(UUID inventoryItemId, UnitStatus status);

    long countByStatus(UnitStatus status);

    @Query("select count(unit) from InventoryUnit unit where unit.status not in :statuses")
    long countByStatusNotIn(@Param("statuses") List<UnitStatus> statuses);

    @EntityGraph(attributePaths = {"inventoryItem", "inventoryItem.location"})
    List<InventoryUnit> findBySkuAndReceiptReferenceIdOrderByCreatedAt(String sku, String receiptReferenceId);

    @Query("select count(unit) from InventoryUnit unit where unit.inventoryItem.id = :itemId and unit.status not in :disposedStatuses")
    long countActiveByItemId(@Param("itemId") UUID itemId, @Param("disposedStatuses") List<UnitStatus> disposedStatuses);

    @EntityGraph(attributePaths = {"inventoryItem", "inventoryItem.location"})
    List<InventoryUnit> findByInventoryItem_IdOrderByCreatedAt(UUID inventoryItemId);
}

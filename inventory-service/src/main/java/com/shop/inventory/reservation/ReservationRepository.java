package com.shop.inventory.reservation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByReferenceId(String referenceId);

    @Query("""
            select reservation from InventoryReservation reservation
            where (:status is null or reservation.status = :status)
              and (:sku is null or reservation.sku = :sku)
              and (:locationId is null or reservation.location.id = :locationId)
            """)
    Page<InventoryReservation> search(@Param("status") ReservationStatus status,
                                      @Param("sku") String sku,
                                      @Param("locationId") UUID locationId,
                                      Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from InventoryReservation reservation join fetch reservation.location where reservation.id = :id")
    Optional<InventoryReservation> findByIdForUpdate(@Param("id") UUID id);

    List<InventoryReservation> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            ReservationStatus status, Instant now);

    boolean existsByLocation_Id(UUID locationId);
}

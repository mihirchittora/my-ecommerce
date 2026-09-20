package com.shop.inventory.reservation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationUnitRepository extends JpaRepository<ReservationUnit, UUID> {
    @EntityGraph(attributePaths = {"inventoryUnit", "inventoryUnit.inventoryItem", "inventoryUnit.inventoryItem.location"})
    List<ReservationUnit> findByReservation_IdOrderByInventoryUnit_UnitCode(UUID reservationId);
}

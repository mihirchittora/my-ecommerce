package com.shop.inventory.location;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<InventoryLocation, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    List<InventoryLocation> findAllByOrderByCodeAsc();
    long countByStatus(LocationStatus status);
}

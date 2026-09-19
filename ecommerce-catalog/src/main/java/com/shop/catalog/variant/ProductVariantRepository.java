package com.shop.catalog.variant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndProduct_IdNot(String sku, UUID productId);
    Optional<ProductVariant> findByIdAndProduct_Id(UUID id, UUID productId);
}

package com.shop.catalog.image;

import com.shop.catalog.product.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    Optional<ProductImage> findByIdAndProduct_Id(UUID id, UUID productId);
}

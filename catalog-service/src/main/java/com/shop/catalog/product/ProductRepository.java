package com.shop.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    boolean existsByCategory_Id(UUID categoryId);
    Page<Product> findByCategory_Id(UUID categoryId, Pageable pageable);
    Page<Product> findByCategory_IdAndNameContainingIgnoreCase(UUID categoryId, String name, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findByCategory_IdAndStatus(UUID categoryId, ProductStatus status, Pageable pageable);
    Page<Product> findByCategory_IdAndStatusAndNameContainingIgnoreCase(UUID categoryId, ProductStatus status, String name, Pageable pageable);
    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name, Pageable pageable);
}

package com.shop.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    boolean existsByCategory_Id(UUID categoryId);
    Page<Product> findByCategory_Id(UUID categoryId, Pageable pageable);
    Page<Product> findByCategory_IdAndNameContainingIgnoreCase(UUID categoryId, String name, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

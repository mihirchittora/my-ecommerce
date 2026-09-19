package com.shop.catalog.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    Optional<Category> findBySlug(String slug);

    boolean existsByNameIgnoreCaseAndParent_Id(String name, UUID parentId);
    boolean existsByNameIgnoreCaseAndParent_IdAndIdNot(String name, UUID parentId, UUID id);
    boolean existsByNameIgnoreCaseAndParentIsNull(String name);
    boolean existsByNameIgnoreCaseAndParentIsNullAndIdNot(String name, UUID id);

    List<Category> findByParent_IdOrderByName(UUID parentId);
    List<Category> findByParentIsNullOrderByName();
}

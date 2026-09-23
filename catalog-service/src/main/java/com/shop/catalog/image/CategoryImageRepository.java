package com.shop.catalog.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryImageRepository extends JpaRepository<CategoryImage, UUID> {
    Optional<CategoryImage> findByCategory_Id(UUID categoryId);

    List<CategoryImage> findByCategory_IdIn(Collection<UUID> categoryIds);
}

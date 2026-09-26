package com.shop.catalog.site;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarouselSlideRepository extends JpaRepository<CarouselSlide, UUID> {
    List<CarouselSlide> findBySettings_IdOrderBySortOrderAsc(UUID settingsId);

    Optional<CarouselSlide> findByIdAndSettings_Id(UUID id, UUID settingsId);
}

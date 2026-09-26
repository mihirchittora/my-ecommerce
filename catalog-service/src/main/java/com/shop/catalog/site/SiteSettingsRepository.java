package com.shop.catalog.site;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, UUID> {
}

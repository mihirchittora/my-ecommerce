package com.shop.catalog.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carousel_slides")
@Getter
@Setter
@NoArgsConstructor
public class CarouselSlide {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_settings_id", nullable = false)
    private SiteSettings settings;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(length = 160)
    private String eyebrow;

    @Column(nullable = false, length = 300)
    private String headline;

    @Column(length = 1000)
    private String description;

    @Column(name = "primary_cta_label", length = 80)
    private String primaryCtaLabel;

    @Column(name = "primary_cta_url", length = 500)
    private String primaryCtaUrl;

    @Column(name = "secondary_cta_label", length = 80)
    private String secondaryCtaLabel;

    @Column(name = "secondary_cta_url", length = 500)
    private String secondaryCtaUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "image_storage_key", length = 1000)
    private String imageStorageKey;

    @Column(name = "image_original_filename", length = 255)
    private String imageOriginalFilename;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(name = "image_size_bytes")
    private Long imageSizeBytes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

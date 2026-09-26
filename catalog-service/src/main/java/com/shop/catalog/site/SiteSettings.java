package com.shop.catalog.site;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "site_settings")
@Getter
@Setter
@NoArgsConstructor
public class SiteSettings {
    public static final UUID DEFAULT_ID = UUID.fromString("6d3c5c7c-13ad-4bca-9f36-6e1c7a700001");

    @Id
    private UUID id;

    @Column(name = "site_title", nullable = false, length = 160)
    private String siteTitle;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "logo_storage_key", length = 1000)
    private String logoStorageKey;

    @Column(name = "logo_original_filename", length = 255)
    private String logoOriginalFilename;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    @Column(name = "logo_size_bytes")
    private Long logoSizeBytes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "settings")
    @org.hibernate.annotations.OrderBy(clause = "sort_order asc")
    private List<CarouselSlide> slides = new ArrayList<>();
}

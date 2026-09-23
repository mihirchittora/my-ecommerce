package com.shop.catalog.image;

import com.shop.catalog.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * The single representative image for a Catalog category. Category media is
 * deliberately separate from ProductImage so ownership stays explicit.
 */
@Entity
@Table(name = "category_images")
@Getter
@Setter
@NoArgsConstructor
public class CategoryImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, unique = true)
    private Category category;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "alt_text", nullable = false, length = 255)
    private String altText;
}

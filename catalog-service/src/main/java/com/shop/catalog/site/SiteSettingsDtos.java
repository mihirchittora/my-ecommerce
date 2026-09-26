package com.shop.catalog.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SiteSettingsDtos {
    private SiteSettingsDtos() {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 160) String siteTitle
    ) {
    }

    public record SlideRequest(
            @NotBlank @Size(max = 300) String headline,
            @Size(max = 160) String eyebrow,
            @Size(max = 1000) String description,
            @Size(max = 80) String primaryCtaLabel,
            @Size(max = 500) String primaryCtaUrl,
            @Size(max = 80) String secondaryCtaLabel,
            @Size(max = 500) String secondaryCtaUrl,
            @Min(0) int sortOrder,
            Boolean active
    ) {
    }

    public record SlideResponse(
            UUID id,
            int sortOrder,
            String eyebrow,
            String headline,
            String description,
            String primaryCtaLabel,
            String primaryCtaUrl,
            String secondaryCtaLabel,
            String secondaryCtaUrl,
            String imageUrl,
            boolean active,
            String originalFilename,
            String contentType,
            Long sizeBytes,
            Instant updatedAt
    ) {
        static SlideResponse from(CarouselSlide slide) {
            return new SlideResponse(
                    slide.getId(),
                    slide.getSortOrder(),
                    slide.getEyebrow(),
                    slide.getHeadline(),
                    slide.getDescription(),
                    slide.getPrimaryCtaLabel(),
                    slide.getPrimaryCtaUrl(),
                    slide.getSecondaryCtaLabel(),
                    slide.getSecondaryCtaUrl(),
                    slide.getImageUrl(),
                    slide.isActive(),
                    slide.getImageOriginalFilename(),
                    slide.getImageContentType(),
                    slide.getImageSizeBytes(),
                    slide.getUpdatedAt()
            );
        }
    }

    public record Response(
            UUID id,
            String siteTitle,
            String logoUrl,
            String logoOriginalFilename,
            String logoContentType,
            Long logoSizeBytes,
            Instant updatedAt,
            List<SlideResponse> slides
    ) {
        static Response from(SiteSettings settings, List<CarouselSlide> slides) {
            return new Response(
                    settings.getId(),
                    settings.getSiteTitle(),
                    settings.getLogoUrl(),
                    settings.getLogoOriginalFilename(),
                    settings.getLogoContentType(),
                    settings.getLogoSizeBytes(),
                    settings.getUpdatedAt(),
                    slides.stream().filter(CarouselSlide::isActive).map(SlideResponse::from).toList()
            );
        }

        static Response fromAdmin(SiteSettings settings, List<CarouselSlide> slides) {
            return new Response(
                    settings.getId(),
                    settings.getSiteTitle(),
                    settings.getLogoUrl(),
                    settings.getLogoOriginalFilename(),
                    settings.getLogoContentType(),
                    settings.getLogoSizeBytes(),
                    settings.getUpdatedAt(),
                    slides.stream().map(SlideResponse::from).toList()
            );
        }
    }
}

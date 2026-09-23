package com.shop.catalog.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.shop.catalog.image.CategoryImage;

import java.util.UUID;

public final class CategoryDtos {
    private CategoryDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 120) String name,
            UUID parentId,
            @Size(max = 150) String slug,
            @Size(max = 500) String description
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 120) String name,
            UUID parentId,
            @Size(max = 150) String slug,
            @Size(max = 500) String description,
            CategoryStatus status
    ) {
    }

    public record Response(
            UUID id,
            UUID parentId,
            String name,
            String slug,
            CategoryStatus status,
            String description,
            ImageResponse image
    ) {
        public static Response from(Category category, CategoryImage image) {
            return new Response(
                    category.getId(),
                    category.getParent() == null ? null : category.getParent().getId(),
                    category.getName(),
                    category.getSlug(),
                    category.getStatus(),
                    category.getDescription(),
                    image == null ? null : ImageResponse.from(image)
            );
        }
    }

    public record ImageResponse(
            UUID id,
            String url,
            String altText
    ) {
        public static ImageResponse from(CategoryImage image) {
            return new ImageResponse(image.getId(), image.getUrl(), image.getAltText());
        }
    }
}

package com.shop.catalog.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class CategoryDtos {
    private CategoryDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 120) String name,
            UUID parentId,
            @Size(max = 150) String slug
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 120) String name,
            UUID parentId,
            @Size(max = 150) String slug,
            CategoryStatus status
    ) {
    }

    public record Response(
            UUID id,
            UUID parentId,
            String name,
            String slug,
            CategoryStatus status
    ) {
        static Response from(Category category) {
            return new Response(
                    category.getId(),
                    category.getParent() == null ? null : category.getParent().getId(),
                    category.getName(),
                    category.getSlug(),
                    category.getStatus()
            );
        }
    }
}

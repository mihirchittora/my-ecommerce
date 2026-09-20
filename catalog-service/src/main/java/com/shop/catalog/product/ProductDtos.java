package com.shop.catalog.product;

import com.shop.catalog.variant.ProductVariant;
import com.shop.catalog.variant.VariantStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {
    }

    public record VariantRequest(
            @NotBlank
            @Size(max = 80)
            @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9._-]{0,79}\\s*$",
                    message = "SKU must contain only letters, numbers, '.', '_' or '-'")
            String sku,

            @NotNull
            @DecimalMin(value = "0.01", inclusive = true)
            @Digits(integer = 17, fraction = 2)
            BigDecimal price,

            @NotNull
            CurrencyCode currency,

            @NotNull
            @Size(max = 50)
            Map<@NotBlank @Size(max = 50) String, @NotBlank @Size(max = 200) String> attributes,

            VariantStatus status
    ) {
    }

    public record ImageRequest(
            @NotBlank @Size(max = 1000) String url,
            @Min(0) int sortOrder
    ) {
    }

    public record CreateRequest(
            @NotNull UUID categoryId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 10000) String description,
            @Size(max = 120) String brand,
            LocalDate expiryDate,
            @Size(max = 220) String slug,
            ProductStatus status,
            @Size(max = 100) @Valid List<VariantRequest> variants,
            @Size(max = 50) @Valid List<ImageRequest> images
    ) {
    }

    public record UpdateRequest(
            @NotNull UUID categoryId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 10000) String description,
            @Size(max = 120) String brand,
            LocalDate expiryDate,
            @Size(max = 220) String slug,
            ProductStatus status,
            @Size(max = 100) @Valid List<VariantRequest> variants,
            @Size(max = 50) @Valid List<ImageRequest> images
    ) {
    }

    public record VariantResponse(
            UUID id,
            String sku,
            BigDecimal price,
            String currency,
            Map<String, String> attributes,
            VariantStatus status
    ) {
        static VariantResponse from(ProductVariant variant) {
            return new VariantResponse(
                    variant.getId(),
                    variant.getSku(),
                    variant.getPrice(),
                    variant.getCurrency(),
                    variant.getAttributes(),
                    variant.getStatus()
            );
        }
    }

    public record ImageResponse(
            UUID id,
            UUID variantId,
            String url,
            int sortOrder,
            String originalFilename,
            String contentType,
            Long sizeBytes
    ) {
        public static ImageResponse from(ProductImage image) {
            return new ImageResponse(
                    image.getId(),
                    image.getVariant() == null ? null : image.getVariant().getId(),
                    image.getUrl(),
                    image.getSortOrder(),
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getSizeBytes()
            );
        }
    }

    public record Response(
            UUID id,
            UUID categoryId,
            String name,
            String slug,
            String description,
            String brand,
            LocalDate expiryDate,
            ProductStatus status,
            List<VariantResponse> variants,
            List<ImageResponse> images
    ) {
        static Response from(Product product) {
            return new Response(
                    product.getId(),
                    product.getCategory().getId(),
                    product.getName(),
                    product.getSlug(),
                    product.getDescription(),
                    product.getBrand(),
                    product.getExpiryDate(),
                    product.getStatus(),
                    product.getVariants().stream().map(VariantResponse::from).toList(),
                    product.getImages().stream().map(ImageResponse::from).toList()
            );
        }
    }
}

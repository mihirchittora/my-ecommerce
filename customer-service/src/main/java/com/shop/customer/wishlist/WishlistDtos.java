package com.shop.customer.wishlist;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class WishlistDtos {
    private WishlistDtos() { }
    public record AddRequest(@NotNull UUID productId, @Size(max = 80) String sku) { }
    public record ItemResponse(UUID id, UUID productId, String sku, Instant createdAt) {
        static ItemResponse from(WishlistItem item) { return new ItemResponse(item.getId(), item.getProductId(), item.getSku(), item.getCreatedAt()); }
    }
}

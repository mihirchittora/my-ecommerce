package com.shop.customer.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    List<WishlistItem> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
    Optional<WishlistItem> findByIdAndCustomer_Id(UUID id, UUID customerId);
    Optional<WishlistItem> findByCustomer_IdAndProductIdAndSku(UUID customerId, UUID productId, String sku);
}

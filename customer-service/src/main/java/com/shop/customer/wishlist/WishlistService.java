package com.shop.customer.wishlist;

import com.shop.customer.common.ConflictException;
import com.shop.customer.common.NotFoundException;
import com.shop.customer.customer.Customer;
import com.shop.customer.customer.CustomerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {
    private final WishlistItemRepository items;
    private final CustomerService customers;
    public WishlistService(WishlistItemRepository items, CustomerService customers) { this.items = items; this.customers = customers; }

    @Transactional(readOnly = true)
    public List<WishlistDtos.ItemResponse> list(Authentication authentication) { return items.findByCustomer_IdOrderByCreatedAtDesc(customer(authentication).getId()).stream().map(WishlistDtos.ItemResponse::from).toList(); }

    @Transactional
    public WishlistDtos.ItemResponse add(Authentication authentication, WishlistDtos.AddRequest request) {
        Customer customer = customer(authentication);
        String sku = request.sku() == null || request.sku().isBlank() ? null : request.sku().trim().toUpperCase(java.util.Locale.ROOT);
        if (items.findByCustomer_IdAndProductIdAndSku(customer.getId(), request.productId(), sku).isPresent()) throw new ConflictException("Product is already in your wishlist");
        WishlistItem item = new WishlistItem(); item.setCustomer(customer); item.setProductId(request.productId()); item.setSku(sku);
        return WishlistDtos.ItemResponse.from(items.save(item));
    }

    @Transactional
    public void remove(Authentication authentication, UUID itemId) { items.delete(items.findByIdAndCustomer_Id(itemId, customer(authentication).getId()).orElseThrow(() -> new NotFoundException("Wishlist item not found"))); }

    private Customer customer(Authentication authentication) { return customers.getOrCreateActive(authentication); }
}

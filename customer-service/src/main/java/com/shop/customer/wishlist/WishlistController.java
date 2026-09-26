package com.shop.customer.wishlist;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/me/wishlist")
@Tag(name = "Wishlist")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {
    private final WishlistService wishlist;
    public WishlistController(WishlistService wishlist) { this.wishlist = wishlist; }
    @GetMapping public List<WishlistDtos.ItemResponse> list(Authentication authentication) { return wishlist.list(authentication); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public WishlistDtos.ItemResponse add(Authentication authentication, @Valid @RequestBody WishlistDtos.AddRequest request) { return wishlist.add(authentication, request); }
    @DeleteMapping("/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(Authentication authentication, @PathVariable UUID itemId) { wishlist.remove(authentication, itemId); }
}

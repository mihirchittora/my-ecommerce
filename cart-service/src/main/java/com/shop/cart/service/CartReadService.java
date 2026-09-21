package com.shop.cart.service;

import com.shop.cart.api.CartDtos;
import com.shop.cart.client.CatalogClient;
import com.shop.cart.client.CatalogSku;
import com.shop.cart.client.CartDependencyException;
import com.shop.cart.client.InactiveSkuException;
import com.shop.cart.client.UnknownSkuException;
import com.shop.cart.domain.Cart;
import com.shop.cart.domain.CartItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CartReadService {
    private final CatalogClient catalog;
    private final String currency;

    public CartReadService(CatalogClient catalog, @Value("${cart.currency:INR}") String currency) {
        this.catalog = catalog;
        this.currency = currency.trim().toUpperCase(Locale.ROOT);
    }

    public CartDtos.CartResponse response(Cart cart) {
        List<CartDtos.CartItemResponse> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean enrichmentAvailable = true;
        long totalQuantity = 0;

        for (CartItem item : cart.getItems()) {
            totalQuantity += item.getQuantity();
            try {
                CatalogSku sku = catalog.getSku(item.getSku());
                if (!sku.active()) {
                    items.add(CartDtos.CartItemResponse.unavailable(item, "Item unavailable because the SKU is inactive"));
                    warnings.add("SKU " + item.getSku() + " is inactive in Catalog");
                } else if (!currency.equalsIgnoreCase(sku.currency())) {
                    items.add(CartDtos.CartItemResponse.unavailable(item, "Item unavailable because its currency differs from the cart"));
                    warnings.add("SKU " + item.getSku() + " has a currency that does not match the cart");
                } else {
                    items.add(CartDtos.CartItemResponse.enriched(item, sku));
                }
            } catch (UnknownSkuException ex) {
                items.add(CartDtos.CartItemResponse.unavailable(item, "Item unavailable because the SKU is no longer in Catalog"));
                warnings.add("SKU " + item.getSku() + " is no longer recognized by Catalog");
            } catch (InactiveSkuException ex) {
                items.add(CartDtos.CartItemResponse.unavailable(item, "Item unavailable because the SKU is inactive"));
                warnings.add("SKU " + item.getSku() + " is inactive in Catalog");
            } catch (CartDependencyException ex) {
                enrichmentAvailable = false;
                items.add(CartDtos.CartItemResponse.raw(item, "Catalog is temporarily unavailable; only SKU and quantity are available"));
                warnings.add("Catalog enrichment is unavailable");
            }
        }

        return new CartDtos.CartResponse(cart.getId(), cart.getCustomerId(), cart.getStatus(), cart.getCurrency(),
                cart.getCreatedAt(), cart.getUpdatedAt(), cart.getExpiresAt(), cart.getConvertedOrderId(),
                cart.getConvertedOrderNumber(), cart.getVersion(), items.size(), totalQuantity,
                enrichmentAvailable, warnings, items);
    }

    public CartDtos.CartSummaryResponse summary(Cart cart) {
        long totalQuantity = cart.getItems().stream().mapToLong(CartItem::getQuantity).sum();
        return new CartDtos.CartSummaryResponse(cart.getId(), cart.getCustomerId(), cart.getStatus(), cart.getCurrency(),
                cart.getCreatedAt(), cart.getUpdatedAt(), cart.getExpiresAt(), cart.getConvertedOrderId(),
                cart.getConvertedOrderNumber(), cart.getItems().size(), totalQuantity);
    }
}

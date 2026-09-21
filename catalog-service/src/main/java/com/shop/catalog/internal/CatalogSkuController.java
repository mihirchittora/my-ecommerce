package com.shop.catalog.internal;

import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.product.ProductStatus;
import com.shop.catalog.variant.ProductVariant;
import com.shop.catalog.variant.ProductVariantRepository;
import com.shop.catalog.variant.VariantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/catalog/skus")
@RequiredArgsConstructor
public class CatalogSkuController {
    private final ProductVariantRepository productVariantRepository;

    @GetMapping("/{sku}")
    @Transactional(readOnly = true)
    public CatalogSkuResponse getSku(@PathVariable String sku) {
        String normalizedSku = sku == null ? "" : sku.trim().toUpperCase(Locale.ROOT);
        ProductVariant variant = productVariantRepository.findBySku(normalizedSku)
                .orElseThrow(() -> new NotFoundException("SKU not found: " + normalizedSku));

        boolean active = variant.getStatus() == VariantStatus.ACTIVE
                && variant.getProduct().getStatus() == ProductStatus.ACTIVE;
        Map<String, String> attributes = variant.getAttributes() == null ? Map.of() : variant.getAttributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
        String variantName = attributes.values().stream().collect(Collectors.joining(" / "));
        return new CatalogSkuResponse(
                variant.getSku(),
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variantName,
                attributes,
                variant.getPrice(),
                variant.getCurrency(),
                active
        );
    }
}

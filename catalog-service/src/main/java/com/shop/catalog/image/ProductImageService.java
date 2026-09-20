package com.shop.catalog.image;

import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.product.Product;
import com.shop.catalog.product.ProductImage;
import com.shop.catalog.product.ProductRepository;
import com.shop.catalog.product.ProductDtos;
import com.shop.catalog.variant.ProductVariant;
import com.shop.catalog.variant.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Transactional
public class ProductImageService {
    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ProductVariantRepository variants;
    private final ImageStorageService storage;

    public ProductImageService(ProductRepository products,
                               ProductImageRepository images,
                               ProductVariantRepository variants,
                               ImageStorageService storage) {
        this.products = products;
        this.images = images;
        this.variants = variants;
        this.storage = storage;
    }

    public ProductDtos.ImageResponse upload(UUID productId, MultipartFile file, int sortOrder, UUID variantId) {
        Product product = product(productId);
        ProductVariant variant = null;
        if (variantId != null) {
            variant = variants.findByIdAndProduct_Id(variantId, productId)
                    .orElseThrow(() -> new NotFoundException("Variant not found for product: " + variantId));
        }

        ImageStorageService.StoredImage stored = storage.store(file, productId.toString());
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setVariant(variant);
        image.setUrl("/api/v1/products/" + productId + "/images/pending/file");
        image.setStorageKey(stored.storageKey());
        image.setOriginalFilename(stored.originalFilename());
        image.setContentType(stored.contentType());
        image.setSizeBytes(stored.size());
        image.setSortOrder(Math.max(sortOrder, 0));

        try {
            ProductImage saved = images.saveAndFlush(image);
            saved.setUrl("/api/v1/products/" + productId + "/images/" + saved.getId() + "/file");
            saved = images.saveAndFlush(saved);
            return ProductDtos.ImageResponse.from(saved);
        } catch (RuntimeException ex) {
            storage.delete(stored.storageKey());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ProductImage get(UUID productId, UUID imageId) {
        return images.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new NotFoundException("Image not found: " + imageId));
    }

    public void delete(UUID productId, UUID imageId) {
        ProductImage image = get(productId, imageId);
        storage.delete(image.getStorageKey());
        images.delete(image);
    }

    private Product product(UUID id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }
}

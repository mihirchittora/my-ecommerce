package com.shop.catalog.product;

import com.shop.catalog.category.Category;
import com.shop.catalog.category.CategoryRepository;
import com.shop.catalog.common.ConflictException;
import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.image.ImageStorageService;
import com.shop.catalog.variant.ProductVariant;
import com.shop.catalog.variant.ProductVariantRepository;
import com.shop.catalog.variant.VariantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class ProductService {
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductVariantRepository variants;
    private final ImageStorageService imageStorage;

    public ProductService(
            ProductRepository products,
            CategoryRepository categories,
            ProductVariantRepository variants,
            ImageStorageService imageStorage) {
        this.products = products;
        this.categories = categories;
        this.variants = variants;
        this.imageStorage = imageStorage;
    }

    public ProductDtos.Response create(ProductDtos.CreateRequest request) {
        Category category = category(request.categoryId());
        Product product = new Product();
        product.setCategory(category);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setSlug(uniqueSlug(request.slug(), request.name(), null));
        product.setStatus(request.status() == null ? ProductStatus.DRAFT : request.status());
        replaceChildren(product, request.variants(), request.images());
        return ProductDtos.Response.from(products.save(product));
    }

    @Transactional(readOnly = true)
    public ProductDtos.Response get(UUID id) {
        return ProductDtos.Response.from(product(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductDtos.Response> list(UUID categoryId, String search, int page, int size, String sortExpression) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sortExpression));
        Page<Product> resultPage;
        if (categoryId != null && search != null && !search.isBlank()) {
            resultPage = products.findByCategory_IdAndNameContainingIgnoreCase(categoryId, search.trim(), pageable);
        } else if (categoryId != null) {
            resultPage = products.findByCategory_Id(categoryId, pageable);
        } else if (search != null && !search.isBlank()) {
            resultPage = products.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            resultPage = products.findAll(pageable);
        }
        return resultPage.map(ProductDtos.Response::from);
    }

    public ProductDtos.Response update(UUID id, ProductDtos.UpdateRequest request) {
        Product product = product(id);
        product.setCategory(category(request.categoryId()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setSlug(uniqueSlug(request.slug(), request.name(), id));
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        replaceChildren(product, request.variants(), request.images());
        return ProductDtos.Response.from(products.save(product));
    }

    public void delete(UUID id) {
        Product product = product(id);
        product.getImages().stream()
                .map(ProductImage::getStorageKey)
                .filter(key -> key != null && !key.isBlank())
                .forEach(imageStorage::delete);
        products.delete(product);
    }

    private void replaceChildren(Product product,
                                 List<ProductDtos.VariantRequest> variantRequests,
                                 List<ProductDtos.ImageRequest> imageRequests) {
        Map<String, ProductVariant> existingBySku = new HashMap<>();
        product.getVariants().forEach(variant ->
                existingBySku.put(normalizeSku(variant.getSku()), variant));

        List<ProductVariant> desiredVariants = new java.util.ArrayList<>();
        if (variantRequests != null) {
            Set<String> seenSkus = new HashSet<>();
            for (ProductDtos.VariantRequest request : variantRequests) {
                String sku = normalizeSku(request.sku());
                if (!seenSkus.add(sku)) {
                    throw new ConflictException("Duplicate SKU in request: " + sku);
                }
                boolean duplicateInDatabase = product.getId() == null
                        ? variants.existsBySku(sku)
                        : variants.existsBySkuAndProduct_IdNot(sku, product.getId());
                if (duplicateInDatabase) {
                    throw new ConflictException("SKU already exists: " + sku);
                }

                ProductVariant variant = existingBySku.remove(sku);
                if (variant == null) {
                    variant = new ProductVariant();
                }
                variant.setProduct(product);
                variant.setSku(sku);
                variant.setPrice(request.price());
                if (request.currency() == null) {
                    throw new com.shop.catalog.common.BadRequestException("Currency is required");
                }
                variant.setCurrency(request.currency().name());
                variant.setAttributes(request.attributes() == null
                        ? new HashMap<>()
                        : new HashMap<>(request.attributes()));
                variant.setStatus(request.status() == null ? VariantStatus.ACTIVE : request.status());
                desiredVariants.add(variant);
            }
        }

        product.getVariants().clear();
        product.getVariants().addAll(desiredVariants);

        if (imageRequests != null) {
            product.getImages().clear();
            for (ProductDtos.ImageRequest request : imageRequests) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setUrl(request.url().trim());
                image.setSortOrder(Math.max(request.sortOrder(), 0));
                product.getImages().add(image);
            }
        }
    }

    private Sort parseSort(String sortExpression) {
        String expression = sortExpression == null || sortExpression.isBlank() ? "name,asc" : sortExpression.trim();
        String[] parts = expression.split(",", 2);
        String property = parts[0].trim();
        Sort.Direction direction;
        try {
            direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException ex) {
            throw new com.shop.catalog.common.BadRequestException("Invalid sort direction: " + (parts.length > 1 ? parts[1] : ""));
        }
        Set<String> allowed = Set.of("name", "brand", "slug", "createdAt", "updatedAt");
        if (!allowed.contains(property)) {
            throw new com.shop.catalog.common.BadRequestException("Invalid sort property: " + property);
        }
        return Sort.by(direction, property);
    }

    private String normalizeSku(String sku) {
        if (sku == null) {
            throw new com.shop.catalog.common.BadRequestException("SKU is required");
        }
        String normalized = sku.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.equals("STRING")
                || !normalized.matches("^[A-Z0-9][A-Z0-9._-]{0,79}$")) {
            throw new com.shop.catalog.common.BadRequestException(
                    "SKU must contain only letters, numbers, '.', '_' or '-' and must not be 'string'");
        }
        return normalized;
    }

    private Product product(UUID id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private Category category(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    private String uniqueSlug(String requestedSlug, String fallbackName, UUID currentId) {
        String raw = requestedSlug == null || requestedSlug.isBlank() ? fallbackName : requestedSlug;
        String base = slugify(raw);
        if (base.isBlank()) {
            base = "product";
        }
        String candidate = base;
        int suffix = 2;
        while (currentId == null
                ? products.existsBySlug(candidate)
                : products.existsBySlugAndIdNot(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = NON_ALNUM.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}

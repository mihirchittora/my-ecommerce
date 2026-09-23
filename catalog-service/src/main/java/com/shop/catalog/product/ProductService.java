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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
        product.setExpiryDate(request.expiryDate());
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
    public ProductDtos.Response getBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new NotFoundException("Product not found");
        }
        return ProductDtos.Response.from(products.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug)));
    }

    @Transactional(readOnly = true)
    public Page<ProductDtos.Response> list(UUID categoryId, String search, int page, int size, String sortExpression) {
        return list(categoryId, search, null, null, null, List.of(), List.of(), page, size, sortExpression);
    }

    @Transactional(readOnly = true)
    public Page<ProductDtos.Response> list(UUID categoryId, String search, ProductStatus status,
                                           BigDecimal priceMin, BigDecimal priceMax,
                                           List<String> brands, List<String> attributes,
                                           int page, int size, String sortExpression) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sortExpression));
        return products.findAll(specification(categoryId, search, status, priceMin, priceMax, brands, attributes), pageable)
                .map(ProductDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public ProductDtos.FacetsResponse facets(UUID categoryId, String search, ProductStatus status) {
        List<Product> catalog = products.findAll(specification(categoryId, search, status, null, null, List.of(), List.of()));
        Set<String> brands = catalog.stream()
                .map(Product::getBrand)
                .filter(brand -> brand != null && !brand.isBlank())
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        Map<String, Set<String>> attributes = new java.util.TreeMap<>();
        List<BigDecimal> prices = new ArrayList<>();
        catalog.stream()
                .flatMap(product -> product.getVariants().stream())
                .filter(variant -> variant.getStatus() == com.shop.catalog.variant.VariantStatus.ACTIVE)
                .forEach(variant -> {
                    if (variant.getPrice() != null) prices.add(variant.getPrice());
                    if (variant.getAttributes() != null) {
                        variant.getAttributes().forEach((key, value) -> attributes
                                .computeIfAbsent(key, ignored -> new java.util.TreeSet<>()).add(value));
                    }
                });
        Map<String, List<String>> orderedAttributes = attributes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left, LinkedHashMap::new));
        BigDecimal min = prices.stream().min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = prices.stream().max(Comparator.naturalOrder()).orElse(null);
        return new ProductDtos.FacetsResponse(new ProductDtos.PriceFacet(min, max), List.copyOf(brands), orderedAttributes);
    }

    public ProductDtos.Response update(UUID id, ProductDtos.UpdateRequest request) {
        Product product = product(id);
        product.setCategory(category(request.categoryId()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setExpiryDate(request.expiryDate());
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
        product.getVariants().forEach(variant -> {
            // Existing rows may predate SKU validation (for example, a Swagger
            // placeholder of "string"). Reconcile them leniently so a valid
            // update can replace the legacy row; request SKUs remain strict.
            if (variant.getSku() != null) {
                existingBySku.put(variant.getSku().trim().toUpperCase(Locale.ROOT), variant);
            }
        });

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

    private Specification<Product> specification(UUID categoryId, String search, ProductStatus status,
                                                 BigDecimal priceMin, BigDecimal priceMax,
                                                 List<String> brands, List<String> attributes) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(root.get("category").get("id").in(categoryAndDescendantIds(categoryId)));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + search.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));
            if (brands != null && !brands.isEmpty()) {
                predicates.add(criteriaBuilder.lower(root.get("brand")).in(brands.stream()
                        .filter(brand -> brand != null && !brand.isBlank())
                        .map(brand -> brand.toLowerCase(Locale.ROOT)).toList()));
            }
            if ((priceMin != null && priceMin.signum() >= 0) || (priceMax != null && priceMax.signum() >= 0) || (attributes != null && !attributes.isEmpty())) {
                var variant = root.join("variants");
                query.distinct(true);
                predicates.add(criteriaBuilder.equal(variant.get("status"), VariantStatus.ACTIVE));
                if (priceMin != null && priceMin.signum() >= 0) predicates.add(criteriaBuilder.greaterThanOrEqualTo(variant.get("price"), priceMin));
                if (priceMax != null && priceMax.signum() >= 0) predicates.add(criteriaBuilder.lessThanOrEqualTo(variant.get("price"), priceMax));
                for (String attribute : attributes == null ? List.<String>of() : attributes) {
                    int separator = attribute == null ? -1 : attribute.indexOf(':');
                    if (separator <= 0 || separator == attribute.length() - 1) continue;
                    String key = attribute.substring(0, separator).trim();
                    String value = attribute.substring(separator + 1).trim();
                    if (key.isBlank() || value.isBlank()) continue;
                    var jsonValue = criteriaBuilder.function("jsonb_extract_path_text", String.class,
                            variant.get("attributes"), criteriaBuilder.literal(key));
                    predicates.add(criteriaBuilder.equal(jsonValue, value));
                }
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Set<UUID> categoryAndDescendantIds(UUID categoryId) {
        Map<UUID, List<UUID>> childrenByParent = new HashMap<>();
        for (Category category : categories.findAll()) {
            if (category.getParent() != null) {
                childrenByParent.computeIfAbsent(category.getParent().getId(), ignored -> new ArrayList<>()).add(category.getId());
            }
        }

        Set<UUID> scope = new HashSet<>();
        List<UUID> pending = new ArrayList<>();
        scope.add(categoryId);
        pending.add(categoryId);
        for (int index = 0; index < pending.size(); index++) {
            for (UUID childId : childrenByParent.getOrDefault(pending.get(index), List.of())) {
                if (scope.add(childId)) pending.add(childId);
            }
        }
        return scope;
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

package com.shop.catalog.category;

import com.shop.catalog.common.ConflictException;
import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class CategoryService {
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private final CategoryRepository repository;
    private final ProductRepository products;

    public CategoryService(CategoryRepository repository, ProductRepository products) {
        this.repository = repository;
        this.products = products;
    }

    public CategoryDtos.Response create(CategoryDtos.CreateRequest request) {
        String name = request.name().trim();
        Category parent = request.parentId() == null ? null : find(request.parentId());
        ensureSiblingNameAvailable(name, parent, null);

        Category category = new Category();
        category.setName(name);
        category.setSlug(uniqueSlug(request.slug(), name, null, parent));
        category.setParent(parent);
        category.setStatus(CategoryStatus.ACTIVE);
        return CategoryDtos.Response.from(repository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryDtos.Response get(UUID id) {
        return CategoryDtos.Response.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> list(UUID parentId) {
        if (parentId != null) {
            find(parentId);
        }
        List<Category> categories = parentId == null
                ? repository.findByParentIsNullOrderByName()
                : repository.findByParent_IdOrderByName(parentId);
        return categories.stream().map(CategoryDtos.Response::from).toList();
    }

    public CategoryDtos.Response update(UUID id, CategoryDtos.UpdateRequest request) {
        Category category = find(id);
        String name = request.name().trim();
        Category parent = request.parentId() == null ? null : find(request.parentId());

        if (parent != null && parent.getId().equals(id)) {
            throw new ConflictException("Category cannot be its own parent");
        }
        if (parent != null && isDescendant(parent, id)) {
            throw new ConflictException("Category cannot be moved below one of its descendants");
        }
        ensureSiblingNameAvailable(name, parent, id);

        category.setName(name);
        category.setSlug(uniqueSlug(request.slug(), name, id, parent));
        category.setParent(parent);
        if (request.status() != null) {
            category.setStatus(request.status());
        }
        return CategoryDtos.Response.from(repository.save(category));
    }

    public void delete(UUID id) {
        Category category = find(id);
        if (!repository.findByParent_IdOrderByName(id).isEmpty()) {
            throw new ConflictException("Category cannot be deleted while it has child categories");
        }
        if (products.existsByCategory_Id(id)) {
            throw new ConflictException("Category cannot be deleted while products belong to it");
        }
        repository.delete(category);
    }

    private void ensureSiblingNameAvailable(String name, Category parent, UUID currentId) {
        boolean duplicate = currentId == null
                ? (parent == null
                    ? repository.existsByNameIgnoreCaseAndParentIsNull(name)
                    : repository.existsByNameIgnoreCaseAndParent_Id(name, parent.getId()))
                : (parent == null
                    ? repository.existsByNameIgnoreCaseAndParentIsNullAndIdNot(name, currentId)
                    : repository.existsByNameIgnoreCaseAndParent_IdAndIdNot(name, parent.getId(), currentId));
        if (duplicate) {
            throw new ConflictException("Category with this name already exists under the parent");
        }
    }

    private boolean isDescendant(Category candidateParent, UUID categoryId) {
        Category current = candidateParent;
        Set<UUID> visited = new HashSet<>();
        while (current != null && visited.add(current.getId())) {
            if (current.getId().equals(categoryId)) {
                return true;
            }
            current = current.getParent();
        }
        if (current != null) {
            throw new ConflictException("Category hierarchy already contains a cycle");
        }
        return false;
    }

    private Category find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    private String uniqueSlug(String requestedSlug, String fallbackName, UUID currentId, Category parent) {
        String raw = requestedSlug == null || requestedSlug.isBlank() ? fallbackName : requestedSlug;
        String base = slugify(raw);
        if (base.isBlank()) {
            base = "category";
        }
        String candidate = base;
        int suffix = 2;
        while (true) {
            Category existing = repository.findBySlug(candidate).orElse(null);
            if (existing == null || (currentId != null && existing.getId().equals(currentId))) {
                return candidate;
            }
            if (sameParent(existing.getParent(), parent)) {
                throw new ConflictException("Category with this slug already exists under the parent");
            }
            candidate = base + "-" + suffix++;
        }
    }

    private boolean sameParent(Category first, Category second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getId().equals(second.getId());
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = NON_ALNUM.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}

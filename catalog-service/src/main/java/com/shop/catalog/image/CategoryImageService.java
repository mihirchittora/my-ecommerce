package com.shop.catalog.image;

import com.shop.catalog.category.Category;
import com.shop.catalog.category.CategoryDtos;
import com.shop.catalog.category.CategoryRepository;
import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.common.BadRequestException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Transactional
public class CategoryImageService {
    private final CategoryRepository categories;
    private final CategoryImageRepository images;
    private final ImageStorageService storage;

    public CategoryImageService(CategoryRepository categories,
                                CategoryImageRepository images,
                                ImageStorageService storage) {
        this.categories = categories;
        this.images = images;
        this.storage = storage;
    }

    public CategoryDtos.ImageResponse upload(UUID categoryId, MultipartFile file, String altText) {
        Category category = category(categoryId);
        String effectiveAltText = altText == null || altText.isBlank()
                ? defaultAltText(category)
                : altText.trim();
        if (effectiveAltText.length() > 255) {
            throw new BadRequestException("Alt text must be 255 characters or fewer");
        }

        ImageStorageService.StoredImage stored = storage.store(file, "categories", categoryId.toString());
        CategoryImage replacement = new CategoryImage();
        replacement.setCategory(category);
        replacement.setUrl("/api/v1/categories/" + categoryId + "/image/file");
        replacement.setStorageKey(stored.storageKey());
        replacement.setOriginalFilename(stored.originalFilename());
        replacement.setContentType(stored.contentType());
        replacement.setSizeBytes(stored.size());
        replacement.setAltText(effectiveAltText);

        CategoryImage current = images.findByCategory_Id(categoryId).orElse(null);
        try {
            if (current != null) {
                String previousStorageKey = current.getStorageKey();
                current.setUrl(replacement.getUrl());
                current.setStorageKey(replacement.getStorageKey());
                current.setOriginalFilename(replacement.getOriginalFilename());
                current.setContentType(replacement.getContentType());
                current.setSizeBytes(replacement.getSizeBytes());
                current.setAltText(replacement.getAltText());
                CategoryImage saved = images.saveAndFlush(current);
                storage.delete(previousStorageKey);
                return CategoryDtos.ImageResponse.from(saved);
            }
            CategoryImage saved = images.saveAndFlush(replacement);
            return CategoryDtos.ImageResponse.from(saved);
        } catch (RuntimeException ex) {
            storage.delete(stored.storageKey());
            throw ex;
        }
    }

    public CategoryDtos.ImageResponse updateAltText(UUID categoryId, AltTextRequest request) {
        CategoryImage image = get(categoryId);
        image.setAltText(request.altText().trim());
        return CategoryDtos.ImageResponse.from(images.save(image));
    }

    public void delete(UUID categoryId) {
        CategoryImage image = images.findByCategory_Id(categoryId).orElseThrow(
                () -> new NotFoundException("Category image not found: " + categoryId));
        storage.delete(image.getStorageKey());
        images.delete(image);
    }

    public void deleteIfPresent(UUID categoryId) {
        images.findByCategory_Id(categoryId).ifPresent(image -> {
            storage.delete(image.getStorageKey());
            images.delete(image);
        });
    }

    @Transactional(readOnly = true)
    public CategoryImage get(UUID categoryId) {
        return images.findByCategory_Id(categoryId)
                .orElseThrow(() -> new NotFoundException("Category image not found: " + categoryId));
    }

    private Category category(UUID categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + categoryId));
    }

    private String defaultAltText(Category category) {
        return category.getName().trim() + " collection";
    }

    public record AltTextRequest(@NotBlank @Size(max = 255) String altText) {
    }
}

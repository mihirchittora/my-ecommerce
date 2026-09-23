package com.shop.catalog.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import com.shop.catalog.image.CategoryImageService;
import com.shop.catalog.image.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Categories", description = "Product category hierarchy management")
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;
    private final CategoryImageService imageService;
    private final ImageStorageService imageStorage;

    public CategoryController(CategoryService service,
                              CategoryImageService imageService,
                              ImageStorageService imageStorage) {
        this.service = service;
        this.imageService = imageService;
        this.imageStorage = imageStorage;
    }

    @Operation(summary = "Create a category")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDtos.Response create(@Valid @RequestBody CategoryDtos.CreateRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Get a category by ID")
    @GetMapping("/{id}")
    public CategoryDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Get a category by public slug")
    @GetMapping("/slug/{slug}")
    public CategoryDtos.Response getBySlug(@PathVariable String slug) {
        return service.getBySlug(slug);
    }

    @Operation(summary = "List categories", description = "List root categories or children of a parent category")
    @GetMapping
    public List<CategoryDtos.Response> list(@RequestParam(required = false) UUID parentId) {
        return service.list(parentId);
    }

    @Operation(summary = "Update a category")
    @PutMapping("/{id}")
    public CategoryDtos.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryDtos.UpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Upload or replace a category image")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoryDtos.ImageResponse uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String altText) {
        return imageService.upload(id, file, altText);
    }

    @Operation(summary = "Update category image alt text")
    @PutMapping("/{id}/image")
    public CategoryDtos.ImageResponse updateImageAltText(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryImageService.AltTextRequest request) {
        return imageService.updateAltText(id, request);
    }

    @Operation(summary = "Download the current category image")
    @GetMapping("/{id}/image/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> imageFile(@PathVariable UUID id) throws java.io.IOException {
        var image = imageService.get(id);
        var path = imageServicePath(image);
        if (!java.nio.file.Files.exists(path)) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        var resource = new org.springframework.core.io.UrlResource(path.toUri());
        var mediaType = image.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.getContentType());
        return org.springframework.http.ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(java.nio.file.Files.size(path))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(
                                image.getOriginalFilename() == null ? "category-image" : image.getOriginalFilename()).build().toString())
                .body(resource);
    }

    @Operation(summary = "Remove the current category image")
    @DeleteMapping("/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable UUID id) {
        imageService.delete(id);
    }

    private java.nio.file.Path imageServicePath(com.shop.catalog.image.CategoryImage image) {
        // The storage abstraction is intentionally kept behind the Catalog
        // service; this controller only needs the resolved, validated path.
        return imageStorage.resolve(image.getStorageKey());
    }

    @Operation(summary = "Delete a category")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

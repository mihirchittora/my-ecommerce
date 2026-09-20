package com.shop.catalog.image;

import com.shop.catalog.product.ProductDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@Validated
@Tag(name = "Product Images", description = "Product image upload and management")
@RequestMapping("/api/v1/products/{productId}/images")
public class ProductImageController {
    private final ProductImageService service;
    private final ImageStorageService storage;

    public ProductImageController(ProductImageService service, ImageStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @Operation(summary = "Upload an image", description = "Uploads JPEG, PNG, or WEBP up to 5 MB. Optionally associates the image with a product variant.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDtos.ImageResponse upload(
            @PathVariable UUID productId,
            @Parameter(description = "Image file (JPEG, PNG, WEBP)")
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") @Min(0) int sortOrder,
            @RequestParam(required = false) UUID variantId) {
        return service.upload(productId, file, sortOrder, variantId);
    }

    @Operation(summary = "Download an image")
    @GetMapping("/{imageId}/file")
    public ResponseEntity<Resource> file(@PathVariable UUID productId, @PathVariable UUID imageId) throws IOException {
        var image = service.get(productId, imageId);
        if (image.getStorageKey() == null || image.getStorageKey().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path path = storage.resolve(image.getStorageKey());
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(path.toUri());
        MediaType mediaType = image.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(Files.size(path))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(
                                image.getOriginalFilename() == null ? "image" : image.getOriginalFilename()).build().toString())
                .body(resource);
    }

    @Operation(summary = "Delete an image")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId, @PathVariable UUID imageId) {
        service.delete(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}

package com.shop.catalog.site;

import com.shop.catalog.image.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@Tag(name = "Storefront Settings", description = "Public storefront branding and carousel settings")
@RequestMapping("/api/v1/site-settings")
public class SiteSettingsController {
    private final SiteSettingsService service;
    private final ImageStorageService storage;

    public SiteSettingsController(SiteSettingsService service, ImageStorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @Operation(summary = "Get public storefront settings")
    @GetMapping
    public SiteSettingsDtos.Response get() {
        return service.getPublic();
    }

    @Operation(summary = "Get admin storefront settings")
    @GetMapping("/admin")
    public SiteSettingsDtos.Response getAdmin() {
        return service.getAdmin();
    }

    @Operation(summary = "Update the storefront title")
    @PutMapping
    public SiteSettingsDtos.Response update(@Valid @RequestBody SiteSettingsDtos.UpdateRequest request) {
        return service.update(request);
    }

    @Operation(summary = "Upload or replace the storefront logo")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SiteSettingsDtos.Response uploadLogo(@RequestParam("file") MultipartFile file) {
        return service.uploadLogo(file);
    }

    @Operation(summary = "Remove the storefront logo")
    @DeleteMapping("/logo")
    public SiteSettingsDtos.Response deleteLogo() {
        return service.deleteLogo();
    }

    @Operation(summary = "Download the storefront logo")
    @GetMapping("/logo/file")
    public ResponseEntity<Resource> logoFile() throws IOException {
        SiteSettings settings = service.logoForFile();
        if (settings.getLogoStorageKey() == null || settings.getLogoStorageKey().isBlank()) return ResponseEntity.notFound().build();
        return fileResponse(storage.resolve(settings.getLogoStorageKey()), settings.getLogoContentType(), settings.getLogoOriginalFilename());
    }

    @Operation(summary = "Create a carousel slide")
    @PostMapping("/slides")
    @ResponseStatus(CREATED)
    public SiteSettingsDtos.SlideResponse createSlide(@Valid @RequestBody SiteSettingsDtos.SlideRequest request) {
        return service.createSlide(request);
    }

    @Operation(summary = "Update a carousel slide")
    @PutMapping("/slides/{slideId}")
    public SiteSettingsDtos.SlideResponse updateSlide(@PathVariable UUID slideId, @Valid @RequestBody SiteSettingsDtos.SlideRequest request) {
        return service.updateSlide(slideId, request);
    }

    @Operation(summary = "Upload or replace a carousel slide image")
    @PostMapping(value = "/slides/{slideId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SiteSettingsDtos.SlideResponse uploadSlideImage(@PathVariable UUID slideId, @RequestParam("file") MultipartFile file) {
        return service.uploadSlideImage(slideId, file);
    }

    @Operation(summary = "Remove a carousel slide image")
    @DeleteMapping("/slides/{slideId}/image")
    public SiteSettingsDtos.SlideResponse deleteSlideImage(@PathVariable UUID slideId) {
        return service.deleteSlideImage(slideId);
    }

    @Operation(summary = "Download a carousel slide image")
    @GetMapping("/slides/{slideId}/file")
    public ResponseEntity<Resource> slideFile(@PathVariable UUID slideId) throws IOException {
        CarouselSlide slide = service.slideForFile(slideId);
        if (slide.getImageStorageKey() == null || slide.getImageStorageKey().isBlank()) return ResponseEntity.notFound().build();
        return fileResponse(storage.resolve(slide.getImageStorageKey()), slide.getImageContentType(), slide.getImageOriginalFilename());
    }

    @Operation(summary = "Delete a carousel slide")
    @DeleteMapping("/slides/{slideId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteSlide(@PathVariable UUID slideId) {
        service.deleteSlide(slideId);
    }

    private ResponseEntity<Resource> fileResponse(Path path, String contentType, String filename) throws IOException {
        if (!Files.exists(path)) return ResponseEntity.notFound().build();
        Resource resource = new UrlResource(path.toUri());
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(Files.size(path))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename == null ? "image" : filename).build().toString())
                .body(resource);
    }
}

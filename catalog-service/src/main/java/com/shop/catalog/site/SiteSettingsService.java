package com.shop.catalog.site;

import com.shop.catalog.common.NotFoundException;
import com.shop.catalog.image.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SiteSettingsService {
    private final SiteSettingsRepository settingsRepository;
    private final CarouselSlideRepository slideRepository;
    private final ImageStorageService storage;

    public SiteSettingsService(SiteSettingsRepository settingsRepository,
                               CarouselSlideRepository slideRepository,
                               ImageStorageService storage) {
        this.settingsRepository = settingsRepository;
        this.slideRepository = slideRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public SiteSettingsDtos.Response getPublic() {
        SiteSettings settings = settings();
        return SiteSettingsDtos.Response.from(settings, slideRepository.findBySettings_IdOrderBySortOrderAsc(settings.getId()));
    }

    @Transactional(readOnly = true)
    public SiteSettingsDtos.Response getAdmin() {
        SiteSettings settings = settings();
        return SiteSettingsDtos.Response.fromAdmin(settings, slideRepository.findBySettings_IdOrderBySortOrderAsc(settings.getId()));
    }

    @Transactional(readOnly = true)
    public SiteSettings logoForFile() {
        return settings();
    }

    public SiteSettingsDtos.Response update(SiteSettingsDtos.UpdateRequest request) {
        SiteSettings settings = settings();
        settings.setSiteTitle(request.siteTitle().trim());
        settings.setUpdatedAt(Instant.now());
        settingsRepository.save(settings);
        return getAdmin();
    }

    public SiteSettingsDtos.Response uploadLogo(MultipartFile file) {
        SiteSettings settings = settings();
        ImageStorageService.StoredImage stored = storage.store(file, "site-settings", "branding");
        String previousStorageKey = settings.getLogoStorageKey();
        try {
            settings.setLogoUrl("/api/v1/site-settings/logo/file");
            settings.setLogoStorageKey(stored.storageKey());
            settings.setLogoOriginalFilename(stored.originalFilename());
            settings.setLogoContentType(stored.contentType());
            settings.setLogoSizeBytes(stored.size());
            settings.setUpdatedAt(Instant.now());
            settingsRepository.saveAndFlush(settings);
            if (previousStorageKey != null) storage.delete(previousStorageKey);
            return getAdmin();
        } catch (RuntimeException ex) {
            storage.delete(stored.storageKey());
            throw ex;
        }
    }

    public SiteSettingsDtos.Response deleteLogo() {
        SiteSettings settings = settings();
        String previousStorageKey = settings.getLogoStorageKey();
        settings.setLogoUrl(null);
        settings.setLogoStorageKey(null);
        settings.setLogoOriginalFilename(null);
        settings.setLogoContentType(null);
        settings.setLogoSizeBytes(null);
        settings.setUpdatedAt(Instant.now());
        settingsRepository.save(settings);
        if (previousStorageKey != null) storage.delete(previousStorageKey);
        return getAdmin();
    }

    public SiteSettingsDtos.SlideResponse createSlide(SiteSettingsDtos.SlideRequest request) {
        SiteSettings settings = settings();
        CarouselSlide slide = new CarouselSlide();
        slide.setSettings(settings);
        apply(slide, request);
        slide.setCreatedAt(Instant.now());
        slide.setUpdatedAt(Instant.now());
        return SiteSettingsDtos.SlideResponse.from(slideRepository.save(slide));
    }

    public SiteSettingsDtos.SlideResponse updateSlide(UUID slideId, SiteSettingsDtos.SlideRequest request) {
        CarouselSlide slide = slide(slideId);
        apply(slide, request);
        slide.setUpdatedAt(Instant.now());
        return SiteSettingsDtos.SlideResponse.from(slideRepository.save(slide));
    }

    public SiteSettingsDtos.SlideResponse uploadSlideImage(UUID slideId, MultipartFile file) {
        CarouselSlide slide = slide(slideId);
        ImageStorageService.StoredImage stored = storage.store(file, "carousel", slideId.toString());
        String previousStorageKey = slide.getImageStorageKey();
        try {
            slide.setImageUrl("/api/v1/site-settings/slides/" + slideId + "/file");
            slide.setImageStorageKey(stored.storageKey());
            slide.setImageOriginalFilename(stored.originalFilename());
            slide.setImageContentType(stored.contentType());
            slide.setImageSizeBytes(stored.size());
            slide.setUpdatedAt(Instant.now());
            CarouselSlide saved = slideRepository.saveAndFlush(slide);
            if (previousStorageKey != null) storage.delete(previousStorageKey);
            return SiteSettingsDtos.SlideResponse.from(saved);
        } catch (RuntimeException ex) {
            storage.delete(stored.storageKey());
            throw ex;
        }
    }

    public SiteSettingsDtos.SlideResponse deleteSlideImage(UUID slideId) {
        CarouselSlide slide = slide(slideId);
        String previousStorageKey = slide.getImageStorageKey();
        slide.setImageUrl(null);
        slide.setImageStorageKey(null);
        slide.setImageOriginalFilename(null);
        slide.setImageContentType(null);
        slide.setImageSizeBytes(null);
        slide.setUpdatedAt(Instant.now());
        CarouselSlide saved = slideRepository.save(slide);
        if (previousStorageKey != null) storage.delete(previousStorageKey);
        return SiteSettingsDtos.SlideResponse.from(saved);
    }

    public void deleteSlide(UUID slideId) {
        CarouselSlide slide = slide(slideId);
        storage.delete(slide.getImageStorageKey());
        slideRepository.delete(slide);
    }

    @Transactional(readOnly = true)
    public String logoStorageKey() {
        return settings().getLogoStorageKey();
    }

    @Transactional(readOnly = true)
    public CarouselSlide slideForFile(UUID slideId) {
        return slide(slideId);
    }

    private void apply(CarouselSlide slide, SiteSettingsDtos.SlideRequest request) {
        slide.setSortOrder(Math.max(request.sortOrder(), 0));
        slide.setEyebrow(blankToNull(request.eyebrow()));
        slide.setHeadline(request.headline().trim());
        slide.setDescription(blankToNull(request.description()));
        slide.setPrimaryCtaLabel(blankToNull(request.primaryCtaLabel()));
        slide.setPrimaryCtaUrl(blankToNull(request.primaryCtaUrl()));
        slide.setSecondaryCtaLabel(blankToNull(request.secondaryCtaLabel()));
        slide.setSecondaryCtaUrl(blankToNull(request.secondaryCtaUrl()));
        if (request.active() != null) slide.setActive(request.active());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SiteSettings settings() {
        return settingsRepository.findById(SiteSettings.DEFAULT_ID)
                .orElseThrow(() -> new NotFoundException("Site settings are not configured"));
    }

    private CarouselSlide slide(UUID slideId) {
        return slideRepository.findByIdAndSettings_Id(slideId, SiteSettings.DEFAULT_ID)
                .orElseThrow(() -> new NotFoundException("Carousel slide not found: " + slideId));
    }
}

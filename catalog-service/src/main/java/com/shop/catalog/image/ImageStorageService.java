package com.shop.catalog.image;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface ImageStorageService {
    StoredImage store(MultipartFile file, String collection, String ownerDirectory);

    void delete(String storageKey);

    Path resolve(String storageKey);

    record StoredImage(String storageKey, String originalFilename, String contentType, long size) {
    }
}

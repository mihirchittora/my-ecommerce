package com.shop.catalog.image;

import com.shop.catalog.common.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalImageStorageService implements ImageStorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path root;
    private final long maxBytes;

    public LocalImageStorageService(
            @Value("${app.images.storage-dir:./uploads}") String storageDir,
            @Value("${app.images.max-size-bytes:5242880}") long maxBytes) {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize image storage: " + root, ex);
        }
    }

    @Override
    public StoredImage store(MultipartFile file, String collection, String ownerDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Image exceeds maximum size of " + maxBytes + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Unsupported image type. Allowed types: JPEG, PNG, WEBP");
        }
        verifyImageSignature(file, contentType.toLowerCase(Locale.ROOT));

        String extension = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new BadRequestException("Unsupported image type");
        };

        String safeCollection = collection == null ? "" : collection.replaceAll("[^A-Za-z0-9_-]", "");
        String safeOwnerDirectory = ownerDirectory == null ? "" : ownerDirectory.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeCollection.isBlank() || safeOwnerDirectory.isBlank()) {
            throw new BadRequestException("Invalid image directory");
        }

        String filename = UUID.randomUUID() + extension;
        Path directory = root.resolve(safeCollection).resolve(safeOwnerDirectory).normalize();
        if (!directory.startsWith(root)) {
            throw new BadRequestException("Invalid image path");
        }
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid image path");
        }

        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store image", ex);
        }

        String storageKey = root.relativize(target).toString().replace(java.io.File.separatorChar, '/');
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        return new StoredImage(storageKey, originalFilename, contentType, file.getSize());
    }

    private void verifyImageSignature(MultipartFile file, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (contentType) {
                case "image/jpeg" -> header.length >= 3
                        && (header[0] & 0xff) == 0xff
                        && (header[1] & 0xff) == 0xd8
                        && (header[2] & 0xff) == 0xff;
                case "image/png" -> header.length >= 8
                        && (header[0] & 0xff) == 0x89
                        && header[1] == 0x50
                        && header[2] == 0x4e
                        && header[3] == 0x47
                        && header[4] == 0x0d
                        && header[5] == 0x0a
                        && header[6] == 0x1a
                        && header[7] == 0x0a;
                case "image/webp" -> header.length >= 12
                        && header[0] == 'R'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == 'F'
                        && header[8] == 'W'
                        && header[9] == 'E'
                        && header[10] == 'B'
                        && header[11] == 'P';
                default -> false;
            };
            if (!valid) {
                throw new BadRequestException("Uploaded file content does not match its image type");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded image");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Path target = resolve(storageKey);
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to delete stored image", ex);
        }
    }

    @Override
    public Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid image storage key");
        }
        return target;
    }
}

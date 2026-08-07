package com.threadly.media.storage;

import com.threadly.media.config.MediaProperties;
import com.threadly.media.exception.ApiException;
import com.threadly.media.media.MediaKind;
import com.threadly.media.media.MediaTypePolicy;
import com.threadly.media.port.ObjectStorage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Free default storage: Docker named volume / local folder.
 * No third-party cloud account required.
 */
@Component
public class LocalFileObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileObjectStorage.class);

    private final MediaProperties properties;
    private Path root;

    public LocalFileObjectStorage(MediaProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Media storage root={} maxFileBytes={}", root, properties.getMaxFileBytes());
    }

    @Override
    public StoredObject store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is required", HttpStatus.BAD_REQUEST.value());
        }

        String original = sanitizeOriginalName(file.getOriginalFilename());
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        if (MediaTypePolicy.isBlocked(contentType, original)) {
            throw new ApiException(
                    "This file type is not allowed (executables and scripts are blocked)",
                    HttpStatus.BAD_REQUEST.value());
        }

        MediaKind kind = MediaTypePolicy.kindOf(contentType, original);
        long size = file.getSize();
        long categoryMax = MediaTypePolicy.maxBytesFor(kind, properties);
        long absoluteMax = properties.getMaxFileBytes();
        long maxAllowed = Math.min(categoryMax, absoluteMax);

        if (size > maxAllowed) {
            throw new ApiException(
                    "File too large for " + kind + " (max " + human(maxAllowed) + ", got " + human(size) + ")",
                    HttpStatus.PAYLOAD_TOO_LARGE.value());
        }
        if (size > absoluteMax) {
            throw new ApiException(
                    "File exceeds absolute limit of " + human(absoluteMax),
                    HttpStatus.PAYLOAD_TOO_LARGE.value());
        }

        String stored = UUID.randomUUID() + extensionFrom(original, contentType, kind);
        Path target = root.resolve(stored).normalize();
        if (!target.startsWith(root)) {
            throw new ApiException("Invalid storage path", HttpStatus.BAD_REQUEST.value());
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Stored file kind={} size={} name={}", kind, size, stored);
        return new StoredObject(stored, contentType, size, original, kind);
    }

    @Override
    public Resource load(String storedName) {
        try {
            Path file = root.resolve(storedName).normalize();
            if (!file.startsWith(root)) {
                throw new ApiException("Invalid path", HttpStatus.BAD_REQUEST.value());
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException("File not found on disk", HttpStatus.NOT_FOUND.value());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException("File not found", HttpStatus.NOT_FOUND.value());
        }
    }

    private static String sanitizeOriginalName(String name) {
        if (name == null || name.isBlank()) {
            return "upload.bin";
        }
        String clean = name.replace("\\", "/");
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) {
            clean = clean.substring(slash + 1);
        }
        clean = clean.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
        if (clean.length() > 200) {
            clean = clean.substring(clean.length() - 200);
        }
        return clean.isBlank() ? "upload.bin" : clean;
    }

    private static String extensionFrom(String original, String contentType, MediaKind kind) {
        int dot = original.lastIndexOf('.');
        if (dot > 0 && dot < original.length() - 1) {
            String ext = original.substring(dot).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.[a-z0-9]{1,10}")) {
                return ext;
            }
        }
        return switch (kind) {
            case IMAGE -> contentType.contains("png") ? ".png" : ".jpg";
            case VIDEO -> ".mp4";
            case AUDIO -> ".mp3";
            case ARCHIVE -> ".zip";
            case DOCUMENT -> contentType.contains("pdf") ? ".pdf" : ".bin";
            case OTHER -> ".bin";
        };
    }

    private static String human(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        }
        return (bytes / (1024 * 1024)) + " MB";
    }
}

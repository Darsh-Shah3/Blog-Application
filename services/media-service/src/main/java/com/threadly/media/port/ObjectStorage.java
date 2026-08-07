package com.threadly.media.port;

import com.threadly.media.media.MediaKind;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Storage port: keep DB metadata and physical disk operations separate.
 * Swap this for free-tier object storage (MinIO self-host / Cloudflare R2 free tier)
 * later without touching MediaServiceImpl business flow.
 */
public interface ObjectStorage {

    StoredObject store(MultipartFile file) throws IOException;

    Resource load(String storedName);

    record StoredObject(
            String storedName,
            String contentType,
            long sizeBytes,
            String originalName,
            MediaKind kind
    ) {
    }
}

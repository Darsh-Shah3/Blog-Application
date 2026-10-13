package com.threadly.media.storage;

import com.threadly.media.config.MediaProperties;
import com.threadly.media.exception.ApiException;
import com.threadly.media.media.MediaKind;
import com.threadly.media.media.MediaTypePolicy;
import com.threadly.media.port.ObjectStorage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

/**
 * S3-compatible storage (MinIO, Cloudflare R2, AWS S3).
 * Enable with {@code MEDIA_STORAGE=s3} / {@code app.media-storage=s3}.
 */
@Component
@ConditionalOnProperty(name = "app.media-storage", havingValue = "s3")
public class S3ObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);

    private final MediaProperties properties;
    private S3Client client;
    private String bucket;

    public S3ObjectStorage(MediaProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        MediaProperties.S3 s3 = properties.getS3();
        if (s3.getEndpoint() == null || s3.getEndpoint().isBlank()) {
            throw new IllegalStateException("app.s3.endpoint is required when media-storage=s3");
        }
        bucket = s3.getBucket();
        var creds = AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey());
        var s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(s3.isPathStyle())
                .build();
        client = S3Client.builder()
                .endpointOverride(URI.create(s3.getEndpoint()))
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .serviceConfiguration(s3Config)
                .build();
        ensureBucket();
        log.info("S3 media storage endpoint={} bucket={} pathStyle={}",
                s3.getEndpoint(), bucket, s3.isPathStyle());
    }

    @PreDestroy
    void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    private void ensureBucket() {
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created media bucket={}", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created media bucket={}", bucket);
            } else {
                throw e;
            }
        }
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
        long maxAllowed = Math.min(
                MediaTypePolicy.maxBytesFor(kind, properties),
                properties.getMaxFileBytes());
        if (size > maxAllowed) {
            throw new ApiException(
                    "File too large for " + kind + " (max " + human(maxAllowed) + ")",
                    HttpStatus.PAYLOAD_TOO_LARGE.value());
        }

        String stored = UUID.randomUUID() + extensionFrom(original, contentType, kind);
        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(stored)
                            .contentType(contentType)
                            .contentLength(size)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), size));
        } catch (S3Exception e) {
            log.error("S3 put failed key={}: {}", stored, e.getMessage());
            throw new ApiException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        log.debug("Stored S3 object kind={} size={} key={}", kind, size, stored);
        return new StoredObject(stored, contentType, size, original, kind);
    }

    @Override
    public Resource load(String storedName) {
        try {
            var response = client.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(storedName).build());
            long length = response.response().contentLength();
            return new InputStreamResource(response) {
                @Override
                public long contentLength() {
                    return length;
                }

                @Override
                public String getFilename() {
                    return storedName;
                }
            };
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ApiException("File not found", HttpStatus.NOT_FOUND.value());
            }
            log.error("S3 get failed key={}: {}", storedName, e.getMessage());
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

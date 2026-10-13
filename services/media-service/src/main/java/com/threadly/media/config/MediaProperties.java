package com.threadly.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload path + free-tier friendly size caps (bytes).
 * Binaries stay on disk; Postgres only stores metadata.
 */
@ConfigurationProperties(prefix = "app")
public class MediaProperties {

    private String uploadDir = "./uploads";

    /** {@code local} (default disk volume) or {@code s3} (MinIO / R2 / AWS). */
    private String mediaStorage = "local";

    private final S3 s3 = new S3();

    /** Absolute max for any file (also matches servlet multipart). */
    private long maxFileBytes = 50L * 1024 * 1024; // 50 MB

    private long maxImageBytes = 5L * 1024 * 1024;   // 5 MB
    private long maxVideoBytes = 50L * 1024 * 1024;  // 50 MB
    private long maxAudioBytes = 15L * 1024 * 1024;  // 15 MB
    private long maxDocumentBytes = 20L * 1024 * 1024; // 20 MB (pdf, zip, office)
    private long maxOtherBytes = 10L * 1024 * 1024;  // 10 MB

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getMediaStorage() {
        return mediaStorage;
    }

    public void setMediaStorage(String mediaStorage) {
        this.mediaStorage = mediaStorage;
    }

    public S3 getS3() {
        return s3;
    }

    public static class S3 {
        private String endpoint = "";
        private String region = "us-east-1";
        private String bucket = "threadly-media";
        private String accessKey = "";
        private String secretKey = "";
        /** Path-style for MinIO; virtual-hosted for AWS/R2 often false. */
        private boolean pathStyle = true;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isPathStyle() {
            return pathStyle;
        }

        public void setPathStyle(boolean pathStyle) {
            this.pathStyle = pathStyle;
        }
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
        this.maxFileBytes = maxFileBytes;
    }

    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    public void setMaxImageBytes(long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }

    public long getMaxVideoBytes() {
        return maxVideoBytes;
    }

    public void setMaxVideoBytes(long maxVideoBytes) {
        this.maxVideoBytes = maxVideoBytes;
    }

    public long getMaxAudioBytes() {
        return maxAudioBytes;
    }

    public void setMaxAudioBytes(long maxAudioBytes) {
        this.maxAudioBytes = maxAudioBytes;
    }

    public long getMaxDocumentBytes() {
        return maxDocumentBytes;
    }

    public void setMaxDocumentBytes(long maxDocumentBytes) {
        this.maxDocumentBytes = maxDocumentBytes;
    }

    public long getMaxOtherBytes() {
        return maxOtherBytes;
    }

    public void setMaxOtherBytes(long maxOtherBytes) {
        this.maxOtherBytes = maxOtherBytes;
    }
}

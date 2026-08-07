package com.threadly.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Upload path + free-tier friendly size caps (bytes).
 * Binaries stay on disk; Postgres only stores metadata.
 */
@ConfigurationProperties(prefix = "app")
public class MediaProperties {

    private String uploadDir = "./uploads";

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

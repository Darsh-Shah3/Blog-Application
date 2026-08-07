package com.threadly.media.media;

import com.threadly.media.config.MediaProperties;

/**
 * MIME / extension policy for free-volume uploads.
 * Blocks obvious danger (exe, scripts) while allowing common user attachments.
 */
public final class MediaTypePolicy {

    private MediaTypePolicy() {
    }

    public static MediaKind kindOf(String contentType, String originalName) {
        String ct = contentType == null ? "" : contentType.toLowerCase();
        String name = originalName == null ? "" : originalName.toLowerCase();

        if (ct.startsWith("image/") || endsWith(name, ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg")) {
            return MediaKind.IMAGE;
        }
        if (ct.startsWith("video/") || endsWith(name, ".mp4", ".webm", ".mov", ".mkv", ".avi")) {
            return MediaKind.VIDEO;
        }
        if (ct.startsWith("audio/") || endsWith(name, ".mp3", ".wav", ".ogg", ".m4a", ".flac")) {
            return MediaKind.AUDIO;
        }
        if (ct.equals("application/zip")
                || ct.equals("application/x-zip-compressed")
                || ct.equals("application/x-7z-compressed")
                || ct.equals("application/gzip")
                || ct.equals("application/x-rar-compressed")
                || endsWith(name, ".zip", ".7z", ".rar", ".gz", ".tar")) {
            return MediaKind.ARCHIVE;
        }
        if (ct.equals("application/pdf")
                || ct.contains("document")
                || ct.contains("spreadsheet")
                || ct.contains("presentation")
                || ct.equals("text/plain")
                || ct.equals("text/markdown")
                || ct.equals("text/csv")
                || endsWith(name, ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".md", ".csv")) {
            return MediaKind.DOCUMENT;
        }
        return MediaKind.OTHER;
    }

    public static boolean isBlocked(String contentType, String originalName) {
        String name = originalName == null ? "" : originalName.toLowerCase();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        // Never store executables or server-side script packages as "attachments".
        if (endsWith(name, ".exe", ".bat", ".cmd", ".msi", ".dll", ".so", ".sh", ".ps1",
                ".jar", ".war", ".class", ".php", ".jsp", ".asp", ".aspx", ".js", ".vbs")) {
            return true;
        }
        if (ct.contains("x-msdownload") || ct.contains("x-msdos-program") || ct.equals("application/x-msdownload")) {
            return true;
        }
        return false;
    }

    public static long maxBytesFor(MediaKind kind, MediaProperties props) {
        return switch (kind) {
            case IMAGE -> props.getMaxImageBytes();
            case VIDEO -> props.getMaxVideoBytes();
            case AUDIO -> props.getMaxAudioBytes();
            case DOCUMENT, ARCHIVE -> props.getMaxDocumentBytes();
            case OTHER -> props.getMaxOtherBytes();
        };
    }

    private static boolean endsWith(String name, String... suffixes) {
        for (String s : suffixes) {
            if (name.endsWith(s)) {
                return true;
            }
        }
        return false;
    }
}

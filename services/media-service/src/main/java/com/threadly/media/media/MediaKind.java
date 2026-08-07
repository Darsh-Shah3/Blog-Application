package com.threadly.media.media;

/**
 * High-level file family used for size caps and UI presentation.
 * Not a free-form MIME list — keeps policy simple and safe for free disk storage.
 */
public enum MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

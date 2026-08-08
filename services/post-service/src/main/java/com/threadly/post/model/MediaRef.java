package com.threadly.post.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Media attachment embedded on a post (one post can list many). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRef {
    private UUID id;
    private String originalName;
    private String contentType;
    private String mediaKind;
    private String url;
}

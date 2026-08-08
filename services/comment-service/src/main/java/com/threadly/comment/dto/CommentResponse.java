package com.threadly.comment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID postId;
    private Map<String, Object> post;
    private UUID authorId;
    private String authorUsername;
    private Map<String, Object> author;
    private UUID parentId;
    private Map<String, Object> parent;
    private String content;
    private Long score;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}

package com.threadly.comment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private Long parentId;
    private String content;
    private Long score;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}

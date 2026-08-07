package com.threadly.post.dto;

import com.threadly.post.entity.Post;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PostResponse {
    private Long id;
    private Long communityId;
    private String communityName;
    private String communitySlug;
    private Long authorId;
    private String authorUsername;
    private String title;
    private String content;
    private Post.PostType postType;
    private String linkUrl;
    private Long mediaId;
    private Long score;
    private Long commentCount;
    private Instant createdAt;
    private String createdBy;
    private String updatedBy;
}

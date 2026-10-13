package com.threadly.post.dto;

import com.threadly.post.entity.Post;
import com.threadly.post.model.MediaRef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private UUID id;
    private UUID communityId;
    private String communityName;
    private String communitySlug;
    private UUID authorId;
    private String authorUsername;
    private String title;
    private String content;
    private Post.PostType postType;
    private List<String> linkUrls;
    private List<MediaRef> media;
    private Long score;
    private Long commentCount;
    private Instant createdAt;
    private Instant scheduledAt;
    private String status;
    private String createdBy;
    private String updatedBy;
}

package com.threadly.post.dto;

import com.threadly.post.entity.Post;
import com.threadly.post.model.MediaRef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CreatePostRequest {
    @NotNull
    private UUID communityId;

    @NotBlank
    @Size(max = 300)
    private String title;

    private String content;

    private Post.PostType postType = Post.PostType.TEXT;

    /** Multiple external links on one post. */
    private List<String> linkUrls = new ArrayList<>();

    /** Multiple uploaded media items (ids from media-service). */
    private List<MediaRef> media = new ArrayList<>();

    /**
     * Optional creator-written blurb for follower notifications
     * (YouTube-style secondary line under title).
     */
    @Size(max = 500)
    private String notificationSummary;

    /**
     * Optional future publish time (ISO-8601). When set in the future, the post is SCHEDULED
     * and only appears after that time (or via the schedule publisher job).
     */
    private Instant scheduledAt;
}

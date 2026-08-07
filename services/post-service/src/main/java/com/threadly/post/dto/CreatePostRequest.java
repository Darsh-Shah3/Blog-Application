package com.threadly.post.dto;

import com.threadly.post.entity.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {
    @NotNull
    private Long communityId;

    @NotBlank
    @Size(max = 300)
    private String title;

    private String content;

    private Post.PostType postType = Post.PostType.TEXT;

    private String linkUrl;

    private Long mediaId;
}

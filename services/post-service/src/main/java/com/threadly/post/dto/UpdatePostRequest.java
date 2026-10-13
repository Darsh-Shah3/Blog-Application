package com.threadly.post.dto;

import com.threadly.post.entity.Post;
import com.threadly.post.model.MediaRef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdatePostRequest {
    @NotBlank
    @Size(max = 300)
    private String title;

    private String content;

    private Post.PostType postType = Post.PostType.TEXT;

    private List<String> linkUrls = new ArrayList<>();

    private List<MediaRef> media = new ArrayList<>();
}

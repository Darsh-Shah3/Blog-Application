package com.threadly.post.mapper;

import com.threadly.post.dto.PostResponse;
import com.threadly.post.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .communityId(post.getCommunityId())
                .communityName(post.getCommunity() != null ? post.getCommunity().getName() : null)
                .communitySlug(post.getCommunity() != null ? post.getCommunity().getSlug() : null)
                .authorId(post.getAuthorId())
                .authorUsername(post.getAuthor() != null ? post.getAuthor().getUsername() : null)
                .title(post.getTitle())
                .content(post.getContent())
                .postType(post.getPostType())
                .linkUrls(post.getLinkUrls())
                .media(post.getMedia())
                .score(post.getScore())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .scheduledAt(post.getScheduledAt())
                .status(post.getStatus() != null ? post.getStatus().name() : Post.Status.PUBLISHED.name())
                .createdBy(post.getCreatedBy())
                .updatedBy(post.getUpdatedBy())
                .build();
    }
}

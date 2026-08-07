package com.threadly.post.mapper;

import com.threadly.post.dto.PostResponse;
import com.threadly.post.entity.Post;
import com.threadly.post.port.CommunitySummary;
import com.threadly.post.port.UserSummary;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post, CommunitySummary community, UserSummary author) {
        return PostResponse.builder()
                .id(post.getId())
                .communityId(post.getCommunityId())
                .communityName(community != null ? community.name() : null)
                .communitySlug(community != null ? community.slug() : null)
                .authorId(post.getAuthorId())
                .authorUsername(author != null ? author.username() : null)
                .title(post.getTitle())
                .content(post.getContent())
                .postType(post.getPostType())
                .linkUrl(post.getLinkUrl())
                .mediaId(post.getMediaId())
                .score(post.getScore())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .createdBy(post.getCreatedBy())
                .updatedBy(post.getUpdatedBy())
                .build();
    }
}

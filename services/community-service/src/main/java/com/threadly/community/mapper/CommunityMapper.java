package com.threadly.community.mapper;

import com.threadly.community.dto.CommunityResponse;
import com.threadly.community.entity.Community;
import org.springframework.stereotype.Component;

@Component
public class CommunityMapper {

    public CommunityResponse toResponse(Community c, boolean joined) {
        return CommunityResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .creatorId(c.getCreatorId())
                .memberCount(c.getMemberCount())
                .createdAt(c.getCreatedAt())
                .createdBy(c.getCreatedBy())
                .updatedBy(c.getUpdatedBy())
                .joined(joined)
                .build();
    }
}

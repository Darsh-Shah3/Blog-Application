package com.threadly.post.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threadly.post.cache.RedisJsonCache;
import com.threadly.post.entity.Post;
import com.threadly.post.exception.ApiException;
import com.threadly.post.mapper.PostMapper;
import com.threadly.post.port.CommunityPort;
import com.threadly.post.port.UserPort;
import com.threadly.post.repository.PostRepository;
import com.threadly.post.security.AccessControl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplDeleteTest {

    private static final UUID POST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COMMUNITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTHER_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock PostRepository postRepository;
    @Mock CommunityPort communityPort;
    @Mock UserPort userPort;
    @Mock PostMapper postMapper;
    @Mock com.threadly.post.port.AuditPort auditPort;
    @Mock com.threadly.post.port.NotificationPort notificationPort;
    @Mock ObjectMapper objectMapper;
    @Mock ObjectProvider<RedisJsonCache> cacheProvider;
    @InjectMocks PostServiceImpl postService;

    @Test
    void ownerCanDeleteOwnPost() {
        Post post = Post.builder()
                .id(POST_ID)
                .authorId(AUTHOR_ID)
                .communityId(COMMUNITY_ID)
                .title("t")
                .score(0L)
                .commentCount(0L)
                .build();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        try (MockedStatic<AccessControl> ac = mockStatic(AccessControl.class)) {
            ac.when(AccessControl::canModerateContent).thenReturn(false);
            postService.delete(POST_ID, AUTHOR_ID);
        }

        verify(postRepository).delete(post);
    }

    @Test
    void nonOwnerWithoutModRightsCannotDelete() {
        Post post = Post.builder()
                .id(POST_ID)
                .authorId(AUTHOR_ID)
                .communityId(COMMUNITY_ID)
                .title("t")
                .score(0L)
                .commentCount(0L)
                .build();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        try (MockedStatic<AccessControl> ac = mockStatic(AccessControl.class)) {
            ac.when(AccessControl::canModerateContent).thenReturn(false);
            ApiException ex = assertThrows(ApiException.class, () -> postService.delete(POST_ID, OTHER_USER));
            assertEquals(403, ex.getStatus());
        }

        verify(postRepository, never()).delete(any());
    }

    @Test
    void moderatorCanDeleteOthersPost() {
        Post post = Post.builder()
                .id(POST_ID)
                .authorId(AUTHOR_ID)
                .communityId(COMMUNITY_ID)
                .title("t")
                .score(0L)
                .commentCount(0L)
                .build();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        try (MockedStatic<AccessControl> ac = mockStatic(AccessControl.class)) {
            ac.when(AccessControl::canModerateContent).thenReturn(true);
            postService.delete(POST_ID, OTHER_USER);
        }

        verify(postRepository).delete(post);
    }
}

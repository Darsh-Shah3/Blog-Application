package com.threadly.post.service.impl;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplDeleteTest {

    @Mock PostRepository postRepository;
    @Mock CommunityPort communityPort;
    @Mock UserPort userPort;
    @Mock PostMapper postMapper;
    @Mock com.threadly.post.port.AuditPort auditPort;
    @Mock com.threadly.post.port.NotificationPort notificationPort;
    @InjectMocks PostServiceImpl postService;

    @Test
    void ownerCanDeleteOwnPost() {
        Post post = Post.builder().id(1L).authorId(5L).communityId(2L).title("t").score(0L).commentCount(0L).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        try (MockedStatic<AccessControl> ac = mockStatic(AccessControl.class)) {
            ac.when(AccessControl::canModerateContent).thenReturn(false);
            postService.delete(1L, 5L);
        }

        verify(postRepository).delete(post);
    }

    @Test
    void nonOwnerWithoutModRightsCannotDelete() {
        Post post = Post.builder().id(1L).authorId(5L).communityId(2L).title("t").score(0L).commentCount(0L).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        try (MockedStatic<AccessControl> ac = mockStatic(AccessControl.class)) {
            ac.when(AccessControl::canModerateContent).thenReturn(false);
            ApiException ex = assertThrows(ApiException.class, () -> postService.delete(1L, 9L));
            assertEquals(403, ex.getStatus());
        }

        verify(postRepository, never()).delete(any());
    }

    @Test
    void moderatorCanDeleteOthersPost() {
        Post post = Post.builder().id(1L).authorId(5L).communityId(2L).title("t").score(0L).commentCount(0L).build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        try (MockedStatic<AccessControl> ac = mockStatic(AccessControl.class)) {
            ac.when(AccessControl::canModerateContent).thenReturn(true);
            postService.delete(1L, 9L);
        }

        verify(postRepository).delete(post);
    }
}

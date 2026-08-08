package com.threadly.comment.service.impl;

import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.entity.Comment;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.port.PostPort;
import com.threadly.comment.port.PostSummary;
import com.threadly.comment.port.UserPort;
import com.threadly.comment.port.UserSummary;
import com.threadly.comment.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    private static final UUID POST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTHOR = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock CommentRepository commentRepository;
    @Mock PostPort postPort;
    @Mock UserPort userPort;
    @InjectMocks CommentServiceImpl commentService;

    @Test
    void createAdjustsCommentCountAfterSave() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(POST);
        req.setContent("hello");

        when(postPort.findById(POST)).thenReturn(Optional.of(new PostSummary(POST, "Hi")));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
            return c;
        });
        when(userPort.findById(AUTHOR)).thenReturn(Optional.of(new UserSummary(AUTHOR, "alice")));

        var res = commentService.create(AUTHOR, "alice", req);

        assertEquals(UUID.fromString("33333333-3333-3333-3333-333333333333"), res.getId());
        verify(postPort).adjustCommentCount(POST, 1L);
    }

    @Test
    void createPropagatesCommentCountFailureSoCallerCanRollback() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(POST);
        req.setContent("hello");

        when(postPort.findById(POST)).thenReturn(Optional.of(new PostSummary(POST, "Hi")));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
            return c;
        });
        doThrow(new ApiException("Failed to update post comment count", 503))
                .when(postPort).adjustCommentCount(POST, 1L);

        assertThrows(ApiException.class, () -> commentService.create(AUTHOR, "alice", req));
    }
}

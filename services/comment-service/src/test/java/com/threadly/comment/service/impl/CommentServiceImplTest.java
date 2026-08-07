package com.threadly.comment.service.impl;

import com.threadly.comment.dto.CreateCommentRequest;
import com.threadly.comment.entity.Comment;
import com.threadly.comment.exception.ApiException;
import com.threadly.comment.port.PostPort;
import com.threadly.comment.port.UserPort;
import com.threadly.comment.port.UserSummary;
import com.threadly.comment.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock CommentRepository commentRepository;
    @Mock PostPort postPort;
    @Mock UserPort userPort;
    @InjectMocks CommentServiceImpl commentService;

    @Test
    void createAdjustsCommentCountAfterSave() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(3L);
        req.setContent("hello");

        doNothing().when(postPort).ensureExists(3L);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });
        when(userPort.findById(1L)).thenReturn(Optional.of(new UserSummary(1L, "alice")));

        var res = commentService.create(1L, "alice", req);

        assertEquals(11L, res.getId());
        verify(postPort).adjustCommentCount(3L, 1L);
    }

    @Test
    void createPropagatesCommentCountFailureSoCallerCanRollback() {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setPostId(3L);
        req.setContent("hello");

        doNothing().when(postPort).ensureExists(3L);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(11L);
            return c;
        });
        doThrow(new ApiException("Failed to update post comment count", 503))
                .when(postPort).adjustCommentCount(3L, 1L);

        assertThrows(ApiException.class, () -> commentService.create(1L, "alice", req));
    }
}

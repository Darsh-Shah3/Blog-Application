package com.example.blog_app_apis.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.blog_app_apis.payloads.ApiResponse;
import com.example.blog_app_apis.payloads.CommentDto;
import com.example.blog_app_apis.services.CommentService;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/post/{postId}/user/{userId}/comments")
    public ResponseEntity<CommentDto> createComment(@RequestBody CommentDto commentDto,@PathVariable Integer postId,@PathVariable Integer userId){
        CommentDto comment=this.commentService.createComment(commentDto, postId, userId);
        return new ResponseEntity<CommentDto>(comment, HttpStatus.OK);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable Integer commentId){
        this.commentService.deleteComment(commentId);
        return new ResponseEntity<ApiResponse>(new ApiResponse("Comment deleted successfully!!",true), HttpStatus.OK);
    }
}
 
package com.example.blog_app_apis.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.blog_app_apis.entities.Comment;
import com.example.blog_app_apis.entities.Post;
import com.example.blog_app_apis.entities.User;
import com.example.blog_app_apis.exceptions.ResourceNotFoundException;
import com.example.blog_app_apis.payloads.CommentDto;
import com.example.blog_app_apis.repositories.CommentRepositories;
import com.example.blog_app_apis.repositories.PostRepositories;
import com.example.blog_app_apis.repositories.UserRepositories;
import com.example.blog_app_apis.services.CommentService;

@Service
public class CommentSeviceImpl implements CommentService {

    @Autowired
    private PostRepositories postRepo;
    @Autowired
    private UserRepositories userRepo;
    @Autowired
    private CommentRepositories commentRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CommentDto createComment(CommentDto commentDto, Integer postId, Integer userId) {
        Post post=this.postRepo.findById(postId).orElseThrow(()->new ResourceNotFoundException("Post ", "postId", postId));
        User user=this.userRepo.findById(userId).orElseThrow(()->new ResourceNotFoundException("User ", "userId", userId));
        Comment comment=this.modelMapper.map(commentDto,Comment.class);
        comment.setPost(post);
        comment.setUser(user);
        Comment savedComment=this.commentRepo.save(comment);
        return this.modelMapper.map(savedComment,CommentDto.class );
    }

    @Override
    public void deleteComment(Integer commentId) {
        Comment com=this.commentRepo.findById(commentId).orElseThrow(()->new ResourceNotFoundException("Comment", "commentId", commentId));
        this.commentRepo.delete(com);
    }
    
}

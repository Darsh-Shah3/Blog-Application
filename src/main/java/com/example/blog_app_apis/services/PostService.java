package com.example.blog_app_apis.services;

import java.util.List;

import com.example.blog_app_apis.payloads.PostDto;
import com.example.blog_app_apis.payloads.PostResponse;

public interface PostService {
    // create post
    PostDto createPost(PostDto postDto,Integer userId,Integer categoryId);

    // update post
    PostDto updatePost(PostDto postDto,Integer postId);

    // deletepost
    void deletePost(Integer postId);

    // get all posts
    PostResponse getAllPost(Integer pageNumber,Integer pageSize,String sortBy,String sortDir);

    // get post by id
    PostDto getPostById(Integer postId);

    // get post by category
    PostResponse getPostByCategory(Integer pageNumber,Integer pageSize,Integer categoryId);

    // get post by user
    PostResponse getPostByUser(Integer pageNumber,Integer pageSize,Integer userId);

    // search post
    List<PostDto> searchPosts(String keyword);
}

package com.example.blog_app_apis.services.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.blog_app_apis.entities.Post;
import com.example.blog_app_apis.entities.User;
import com.example.blog_app_apis.entities.Category;
import com.example.blog_app_apis.exceptions.ResourceNotFoundException;
import com.example.blog_app_apis.payloads.PostDto;
import com.example.blog_app_apis.payloads.PostResponse;
import com.example.blog_app_apis.repositories.CategoryRepositories;
import com.example.blog_app_apis.repositories.PostRepositories;
import com.example.blog_app_apis.repositories.UserRepositories;
import com.example.blog_app_apis.services.PostService;

@Service
public class PostServiceImpl implements PostService{

    @Autowired
    private PostRepositories postRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CategoryRepositories categoryRepo;

    @Autowired
    private UserRepositories userRepo;
    
    // create post
    @Override
    public PostDto createPost(PostDto postDto,Integer userId,Integer categoryId) {
        User user=this.userRepo.findById(userId).orElseThrow(()->new ResourceNotFoundException("User ","userId",userId));
        Category category=this.categoryRepo.findById(categoryId).orElseThrow(()->new ResourceNotFoundException("Category ","categoryId",categoryId));
        Post post=this.modelMapper.map(postDto,Post.class);
        post.setImageName("default.png");
        post.setAddedDate(new Date());
        post.setUser(user);
        post.setCategory(category);
        Post newPost=this.postRepo.save(post);
        return this.modelMapper.map(newPost, PostDto.class);
    }

    // update post
    @Override
    public PostDto updatePost(PostDto postDto, Integer postId) {
        Post post=this.postRepo.findById(postId).orElseThrow(()-> new ResourceNotFoundException("Post", "postId", postId));
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setImageName(postDto.getImageName());
        Post updatePost=this.postRepo.save(post);
        return this.modelMapper.map(updatePost,PostDto.class);
    }

    // delete post
    @Override
    public void deletePost(Integer postId) {
        Post post=this.postRepo.findById(postId).orElseThrow(()->new ResourceNotFoundException("Post", "postId", postId));
        this.postRepo.delete(post);
    }

    // get post by id
    @Override
    public PostDto getPostById(Integer postId) {
        Post post=this.postRepo.findById(postId).orElseThrow(()->new ResourceNotFoundException("Post ", "postId", postId));
        PostDto dto=modelMapper.map(post,PostDto.class);
        return dto;
    }

    // get post by categoryid
    @Override
    public PostResponse getPostByCategory(Integer pageNumber,Integer pageSize,Integer categoryId) {
        Category category=this.categoryRepo.findById(categoryId).orElseThrow(()-> new ResourceNotFoundException("Category", "categoryId", categoryId));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Post> pagePosts=this.postRepo.findByCategory(category,pageable);
        List<Post> posts = pagePosts.getContent();
        List<PostDto> postDtos=posts.stream().map((post)-> this.modelMapper.map(post,PostDto.class)).collect(Collectors.toList());
        PostResponse postResponse=new PostResponse();
        postResponse.setContent(postDtos);
        postResponse.setPageNumber(pagePosts.getNumber());
        postResponse.setPageSize(pagePosts.getSize());
        postResponse.setTotalElements(pagePosts.getTotalElements());
        postResponse.setTotalPages(pagePosts.getTotalPages());
        postResponse.setLastPage(pagePosts.isLast());
        return postResponse;
    }

    // get post by userid
    @Override
    public PostResponse getPostByUser(Integer pageNumber,Integer pageSize,Integer userId) {
        User user=this.userRepo.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User", "userId", userId));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Post> pagePosts=this.postRepo.findByUser(user,pageable);
        List<Post> posts = pagePosts.getContent();
        List<PostDto> postDtos=posts.stream().map((post)-> this.modelMapper.map(post,PostDto.class)).collect(Collectors.toList());
        PostResponse postResponse=new PostResponse();
        postResponse.setContent(postDtos);
        postResponse.setPageNumber(pagePosts.getNumber());
        postResponse.setPageSize(pagePosts.getSize());
        postResponse.setTotalElements(pagePosts.getTotalElements());
        postResponse.setTotalPages(pagePosts.getTotalPages());
        postResponse.setLastPage(pagePosts.isLast());
        return postResponse;
    }

    
    // get all posts
    @Override
    public PostResponse getAllPost(Integer pageNumber,Integer pageSize,String sortBy,String sortDir) { 
        Sort sort=(sortDir.equalsIgnoreCase("asc"))?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        // Sort sort = null;
        // if(sortDir.equalsIgnoreCase("asc")){
        //     sort=Sort.by(sortBy).ascending();
        // }else{
        //     sort=Sort.by(sortBy).descending();
        // }
        Pageable p=PageRequest.of(pageNumber, pageSize,sort);
        Page<Post> pagePost=this.postRepo.findAll(p);
        List<Post> allPosts=pagePost.getContent();
        List<PostDto> postDtos=allPosts.stream().map((post)->this.modelMapper.map(post,PostDto.class)).collect(Collectors.toList());
        PostResponse postResponse=new PostResponse();
        postResponse.setContent(postDtos);
        postResponse.setPageNumber(pagePost.getNumber());
        postResponse.setPageSize(pagePost.getSize());
        postResponse.setTotalElements(pagePost.getTotalElements());
        postResponse.setTotalPages(pagePost.getTotalPages());
        postResponse.setLastPage(pagePost.isLast());
        return postResponse;
    }

    // search the posts
    @Override
    public List<PostDto> searchPosts(String keyword) {
        List<Post> posts=this.postRepo.searchByTitle("%"+keyword+"%");
        List<PostDto> postDtos=posts.stream().map((post)->this.modelMapper.map(post,PostDto.class)).collect(Collectors.toList());
        return postDtos;
    }
    
}

package com.example.blog_app_apis.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import com.example.blog_app_apis.config.AppConstants;
import com.example.blog_app_apis.payloads.ApiResponse;
import com.example.blog_app_apis.payloads.PostDto;
import com.example.blog_app_apis.payloads.PostResponse;
import com.example.blog_app_apis.services.FileService;
import com.example.blog_app_apis.services.PostService;


@RestController
@RequestMapping("/api/")
public class PostController {
    @Autowired
    private PostService postService;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;


    // create post
    @PostMapping("/user/{userId}/category/{categoryId}/posts")
    public ResponseEntity<PostDto> createPost(@RequestBody PostDto postDto, @PathVariable Integer userId,
            @PathVariable Integer categoryId) {
        PostDto createPost = this.postService.createPost(postDto, userId, categoryId);
        return new ResponseEntity<>(createPost, HttpStatus.CREATED);
    }

    // get post by user
    // @GetMapping("/user/{userId}/posts")
    // public ResponseEntity<PostResponse>
    // getPostByUser(@RequestParam(value="pageNumber",defaultValue =
    // "0",required=false)int pageNumber,@RequestParam(value="pageSize",defaultValue
    // = "0",required=false)int pageSize,@PathVariable Integer userId){
    // PostResponse
    // posts=this.postService.getPostByUser(pageNumber,pageSize,userId);
    // return new ResponseEntity<PostResponse>(posts,HttpStatus.OK);
    // }
    @GetMapping("/user/{userId}/posts")
    public ResponseEntity<PostResponse> getPostByUser(
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) int pageSize,
            @PathVariable Integer userId) {

        PostResponse posts = postService.getPostByUser(pageNumber, pageSize, userId);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }

    // get post by category
    @GetMapping("/category/{categoryId}/posts")
    public ResponseEntity<PostResponse> getPostByCategory(
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) int pageSize,
            @PathVariable Integer categoryId) {
        PostResponse posts = this.postService.getPostByCategory(pageNumber, pageSize, categoryId);
        return new ResponseEntity<PostResponse>(posts, HttpStatus.OK);
    }

    // get all posts
    @GetMapping("/posts")
    public ResponseEntity<PostResponse> getAllPost(
            @RequestParam(value = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value="sortBy",defaultValue = AppConstants.SORT_BY,required=false)String sortBy,
            @RequestParam(value="sortDir",defaultValue = AppConstants.SORT_DIR,required=false)String sortDir) {
        PostResponse postResponse = this.postService.getAllPost(pageNumber, pageSize,sortBy,sortDir);
        return new ResponseEntity<PostResponse>(postResponse, HttpStatus.OK);
    }

    // get post details
    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostDto> getPostById(@PathVariable Integer postId) {
        PostDto postDto = this.postService.getPostById(postId);
        return new ResponseEntity<PostDto>(postDto, HttpStatus.OK);
    }

    // delete post
    @DeleteMapping("/posts/{postId}")
    public ApiResponse deletePost(@PathVariable Integer postId) {
        this.postService.deletePost(postId);
        return new ApiResponse("Post is deleted successfully", true);
    }

    // update post
    @PutMapping("/posts/{postId}")
    public ResponseEntity<PostDto> updatePost(@RequestBody PostDto postDto, @PathVariable Integer postId) {
        PostDto updatePost = this.postService.updatePost(postDto, postId);
        return new ResponseEntity<PostDto>(updatePost, HttpStatus.OK);
    }

    // search
    @GetMapping("/posts/search/{keywords}")
    public ResponseEntity<List<PostDto>> searchPostByTitle(@PathVariable("keywords") String keywords){
        List<PostDto> result=this.postService.searchPosts(keywords);
        return new ResponseEntity<List<PostDto>>(result,HttpStatus.OK);
    }

    // post image upload
    @PostMapping("/post/image/upload/{postId}")
    public ResponseEntity<PostDto> uploadPostImage(@RequestParam("image") MultipartFile image,@PathVariable Integer postId) throws IOException{
       String fileName = this.fileService.uploadImage(path,image);
       PostDto postDto=this.postService.getPostById(postId);
       postDto.setImageName(fileName);
       PostDto updatePost=this.postService.updatePost(postDto, postId);
       return new ResponseEntity<>(updatePost,HttpStatus.OK);
    }

    // method to serve files
    @GetMapping(value = "post/image/{imageName}",produces=MediaType.IMAGE_JPEG_VALUE)
    public void downloadImage(@PathVariable("imageName") String imageName,HttpServletResponse response) throws IOException{
        InputStream resource=this.fileService.getResource(path, imageName);
        response.setContentType(MediaType.IMAGE_JPEG_VALUE);
        StreamUtils.copy(resource,response.getOutputStream());
    }

}

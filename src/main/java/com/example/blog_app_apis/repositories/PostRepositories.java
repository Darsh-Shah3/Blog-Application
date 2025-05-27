package com.example.blog_app_apis.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.blog_app_apis.entities.Category;
import com.example.blog_app_apis.entities.Post;
import com.example.blog_app_apis.entities.User;

public interface PostRepositories extends JpaRepository<Post,Integer> {
    Page<Post> findByUser(User user,Pageable pageable);
    Page<Post> findByCategory(Category category,Pageable pageable);
    
    @Query("Select p from Post p where p.title like:key")
    List<Post> searchByTitle(@Param("key") String title);
}
 
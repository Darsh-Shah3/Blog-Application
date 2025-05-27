package com.example.blog_app_apis.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.blog_app_apis.entities.Category;

public interface CategoryRepositories extends JpaRepository<Category,Integer>{
    
}

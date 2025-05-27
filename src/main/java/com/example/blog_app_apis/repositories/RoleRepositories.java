package com.example.blog_app_apis.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.blog_app_apis.entities.Role;

public interface RoleRepositories extends JpaRepository<Role,Integer> {
    
}

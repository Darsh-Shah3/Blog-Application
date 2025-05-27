package com.example.blog_app_apis.repositories;
import com.example.blog_app_apis.entities.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepositories extends JpaRepository<User, Integer>  {
    Optional<User> findByEmail(String email);
}

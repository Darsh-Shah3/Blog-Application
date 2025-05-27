package com.example.blog_app_apis.security;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.blog_app_apis.entities.User;
import com.example.blog_app_apis.repositories.UserRepositories;

@Service
public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    private UserRepositories userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // loading user from database by username
        User user=this.userRepo.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        // Convert User to Spring Security's UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList())
        );
    }   
}

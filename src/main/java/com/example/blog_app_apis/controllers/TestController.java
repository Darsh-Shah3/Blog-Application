package com.example.blog_app_apis.controllers;

import org.springframework.web.bind.annotation.*;
import com.example.blog_app_apis.payloads.JwtAuthRequest;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @PostMapping("/login")
    public String testLogin(@RequestBody JwtAuthRequest request) {
        System.out.println("=== TEST ENDPOINT HIT ===");
        System.out.println("Username: " + request.getUsername());
        System.out.println("Password: " + request.getPassword());
        return "Test successful! Username: " + request.getUsername();
    }

    @PostMapping("/simple")
    public String testSimple(@RequestParam String body) {
        System.out.println("=== SIMPLE TEST ENDPOINT HIT ===");
        System.out.println("Raw body: " + body);
        return "Raw body received: " + body;
    }
}
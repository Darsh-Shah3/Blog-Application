package com.example.blog_app_apis.payloads;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class JwtAuthRequest {
    private String username;
    private String password;
}

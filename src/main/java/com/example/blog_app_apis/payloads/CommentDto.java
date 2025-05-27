package com.example.blog_app_apis.payloads;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentDto {
    private Integer id;
    private String content;
}

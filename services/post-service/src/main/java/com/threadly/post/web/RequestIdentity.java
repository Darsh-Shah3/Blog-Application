package com.threadly.post.web;

import com.threadly.post.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestIdentity {

    public UUID requireUserId(UUID headerUserId) {
        if (headerUserId == null) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED.value());
        }
        return headerUserId;
    }
}

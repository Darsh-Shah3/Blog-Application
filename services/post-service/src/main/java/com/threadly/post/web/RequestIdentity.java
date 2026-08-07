package com.threadly.post.web;

import com.threadly.post.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RequestIdentity {

    public Long requireUserId(Long headerUserId) {
        if (headerUserId == null) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED.value());
        }
        return headerUserId;
    }
}

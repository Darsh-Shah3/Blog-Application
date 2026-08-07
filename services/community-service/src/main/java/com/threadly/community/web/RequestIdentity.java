package com.threadly.community.web;

import com.threadly.community.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves caller identity from gateway-injected headers without leaking header names into business logic.
 */
@Component
public class RequestIdentity {

    public Long requireUserId(Long userIdHeader) {
        if (userIdHeader == null) {
            throw new ApiException("Authentication required", HttpStatus.UNAUTHORIZED.value());
        }
        return userIdHeader;
    }
}

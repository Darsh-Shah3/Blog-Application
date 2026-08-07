package com.threadly.auth.service;

import com.threadly.auth.dto.FollowStatusResponse;
import com.threadly.auth.dto.FollowerContactResponse;
import com.threadly.auth.dto.UserResponse;

import java.util.List;

public interface FollowService {
    FollowStatusResponse follow(Long followerId, String targetUsername);

    FollowStatusResponse unfollow(Long followerId, String targetUsername);

    FollowStatusResponse status(Long viewerId, String targetUsername);

    List<UserResponse> followers(String username);

    List<UserResponse> following(String username);

    List<FollowerContactResponse> internalFollowers(Long userId);
}

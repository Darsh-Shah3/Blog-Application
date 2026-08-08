package com.threadly.auth.service;

import com.threadly.auth.dto.FollowStatusResponse;
import com.threadly.auth.dto.FollowerContactResponse;
import com.threadly.auth.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface FollowService {
    FollowStatusResponse follow(UUID followerId, String targetUsername);

    FollowStatusResponse unfollow(UUID followerId, String targetUsername);

    FollowStatusResponse status(UUID viewerId, String targetUsername);

    List<UserResponse> followers(String username);

    List<UserResponse> following(String username);

    List<FollowerContactResponse> internalFollowers(UUID userId);

    List<FollowerContactResponse> internalFollowing(UUID userId);
}

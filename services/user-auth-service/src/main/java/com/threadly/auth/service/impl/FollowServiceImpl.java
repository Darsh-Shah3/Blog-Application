package com.threadly.auth.service.impl;

import com.threadly.auth.dto.FollowStatusResponse;
import com.threadly.auth.dto.FollowerContactResponse;
import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.entity.User;
import com.threadly.auth.entity.UserFollow;
import com.threadly.auth.exception.ApiException;
import com.threadly.auth.mapper.UserMapper;
import com.threadly.auth.repository.UserFollowRepository;
import com.threadly.auth.repository.UserRepository;
import com.threadly.auth.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public FollowStatusResponse follow(Long followerId, String targetUsername) {
        User target = findActiveByUsername(targetUsername);
        if (target.getId().equals(followerId)) {
            throw new ApiException("Cannot follow yourself", HttpStatus.BAD_REQUEST.value());
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, target.getId())) {
            followRepository.save(UserFollow.builder()
                    .followerId(followerId)
                    .followingId(target.getId())
                    .build());
            log.info("User {} followed {}", followerId, target.getUsername());
        }
        return status(followerId, target.getUsername());
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollow(Long followerId, String targetUsername) {
        User target = findActiveByUsername(targetUsername);
        followRepository.findByFollowerIdAndFollowingId(followerId, target.getId())
                .ifPresent(followRepository::delete);
        log.info("User {} unfollowed {}", followerId, target.getUsername());
        return status(followerId, target.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse status(Long viewerId, String targetUsername) {
        User target = findActiveByUsername(targetUsername);
        boolean following = viewerId != null
                && followRepository.existsByFollowerIdAndFollowingId(viewerId, target.getId());
        return FollowStatusResponse.builder()
                .userId(target.getId())
                .username(target.getUsername())
                .following(following)
                .followerCount(followRepository.countByFollowingId(target.getId()))
                .followingCount(followRepository.countByFollowerId(target.getId()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> followers(String username) {
        User target = findActiveByUsername(username);
        return followRepository.findByFollowingIdOrderByCreatedAtDesc(target.getId()).stream()
                .map(f -> userRepository.findById(f.getFollowerId()).orElse(null))
                .filter(u -> u != null && !u.isDeleted())
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> following(String username) {
        User target = findActiveByUsername(username);
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(target.getId()).stream()
                .map(f -> userRepository.findById(f.getFollowingId()).orElse(null))
                .filter(u -> u != null && !u.isDeleted())
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowerContactResponse> internalFollowers(Long userId) {
        return followRepository.findActiveFollowers(userId).stream()
                .map(u -> FollowerContactResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .build())
                .toList();
    }

    private User findActiveByUsername(String username) {
        return userRepository.findActiveByUsername(username.toLowerCase())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));
    }
}

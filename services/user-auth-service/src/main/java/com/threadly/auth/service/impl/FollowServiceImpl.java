package com.threadly.auth.service.impl;

import com.threadly.auth.dto.FollowStatusResponse;
import com.threadly.auth.dto.FollowerContactResponse;
import com.threadly.auth.dto.UserResponse;
import com.threadly.auth.entity.User;
import com.threadly.auth.entity.UserFollow;
import com.threadly.auth.entity.UserFollow.FollowContact;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public FollowStatusResponse follow(UUID followerId, String targetUsername) {
        User follower = findActiveById(followerId);
        User target = findActiveByUsername(targetUsername);
        if (follower.getId().equals(target.getId())) {
            throw new ApiException("Cannot follow yourself", HttpStatus.BAD_REQUEST.value());
        }

        UserFollow followerRow = ensureRow(follower);
        UserFollow targetRow = ensureRow(target);

        if (!contains(followerRow.getFollowing(), target.getId())) {
            followerRow.getFollowing().add(contact(target));
            targetRow.getFollowers().add(contact(follower));
            followRepository.save(followerRow);
            followRepository.save(targetRow);
            log.info("User {} followed {}", follower.getUsername(), target.getUsername());
        }
        return status(followerId, target.getUsername());
    }

    @Override
    @Transactional
    public FollowStatusResponse unfollow(UUID followerId, String targetUsername) {
        User follower = findActiveById(followerId);
        User target = findActiveByUsername(targetUsername);

        UserFollow followerRow = ensureRow(follower);
        UserFollow targetRow = ensureRow(target);

        followerRow.setFollowing(remove(followerRow.getFollowing(), target.getId()));
        targetRow.setFollowers(remove(targetRow.getFollowers(), follower.getId()));
        followRepository.save(followerRow);
        followRepository.save(targetRow);
        log.info("User {} unfollowed {}", follower.getUsername(), target.getUsername());
        return status(followerId, target.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse status(UUID viewerId, String targetUsername) {
        User target = findActiveByUsername(targetUsername);
        UserFollow targetRow = ensureRow(target);
        boolean following = viewerId != null && contains(ensureRow(findActiveById(viewerId)).getFollowing(), target.getId());
        return FollowStatusResponse.builder()
                .userId(target.getId())
                .username(target.getUsername())
                .following(following)
                .followerCount(safeList(targetRow.getFollowers()).size())
                .followingCount(safeList(targetRow.getFollowing()).size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> followers(String username) {
        User target = findActiveByUsername(username);
        return safeList(ensureRow(target).getFollowers()).stream()
                .map(c -> parseUuid(c.getId()))
                .filter(Objects::nonNull)
                .map(userRepository::findById)
                .flatMap(OptionalToStream())
                .filter(u -> !u.isDeleted())
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> following(String username) {
        User target = findActiveByUsername(username);
        return safeList(ensureRow(target).getFollowing()).stream()
                .map(c -> parseUuid(c.getId()))
                .filter(Objects::nonNull)
                .map(userRepository::findById)
                .flatMap(OptionalToStream())
                .filter(u -> !u.isDeleted())
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowerContactResponse> internalFollowers(UUID userId) {
        User user = findActiveById(userId);
        return contactsToResponses(ensureRow(user).getFollowers());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowerContactResponse> internalFollowing(UUID userId) {
        User user = findActiveById(userId);
        return contactsToResponses(ensureRow(user).getFollowing());
    }

    private List<FollowerContactResponse> contactsToResponses(List<FollowContact> contacts) {
        return safeList(contacts).stream()
                .map(c -> parseUuid(c.getId()))
                .filter(Objects::nonNull)
                .map(userRepository::findById)
                .flatMap(OptionalToStream())
                .filter(u -> !u.isDeleted())
                .map(u -> FollowerContactResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .build())
                .toList();
    }

    private UserFollow ensureRow(User user) {
        return followRepository.findByUserId(user.getId())
                .orElseGet(() -> followRepository.save(UserFollow.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .followers(new ArrayList<>())
                        .following(new ArrayList<>())
                        .build()));
    }

    private static FollowContact contact(User user) {
        return FollowContact.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .build();
    }

    private static boolean contains(List<FollowContact> list, UUID id) {
        String key = id.toString();
        return safeList(list).stream().anyMatch(c -> key.equals(c.getId()));
    }

    private static List<FollowContact> remove(List<FollowContact> list, UUID id) {
        String key = id.toString();
        return new ArrayList<>(safeList(list).stream().filter(c -> !key.equals(c.getId())).toList());
    }

    private static List<FollowContact> safeList(List<FollowContact> list) {
        return list == null ? List.of() : list;
    }

    private static UUID parseUuid(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static java.util.function.Function<java.util.Optional<User>, java.util.stream.Stream<User>> OptionalToStream() {
        return opt -> opt.map(java.util.stream.Stream::of).orElseGet(java.util.stream.Stream::empty);
    }

    private User findActiveById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));
        if (user.isDeleted()) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND.value());
        }
        return user;
    }

    private User findActiveByUsername(String username) {
        return userRepository.findActiveByUsername(username.toLowerCase())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));
    }
}

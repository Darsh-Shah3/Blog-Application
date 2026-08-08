package com.threadly.auth.repository;

import com.threadly.auth.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {
    Optional<UserFollow> findByUserId(UUID userId);
}

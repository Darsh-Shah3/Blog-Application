package com.threadly.auth.repository;

import com.threadly.auth.entity.User;
import com.threadly.auth.entity.UserFollow;
import com.threadly.auth.repository.query.AuthQueries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    List<UserFollow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);

    List<UserFollow> findByFollowingIdOrderByCreatedAtDesc(Long followingId);

    @Query(AuthQueries.FOLLOW_FIND_ACTIVE_FOLLOWERS)
    List<User> findActiveFollowers(@Param("userId") Long userId);
}

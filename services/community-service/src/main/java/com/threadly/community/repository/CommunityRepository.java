package com.threadly.community.repository;

import com.threadly.community.entity.Community;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    Optional<Community> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Community> findByNameContainingIgnoreCase(String name, Pageable pageable);
}

package com.threadly.community.repository;

import com.threadly.community.entity.Community;
import com.threadly.community.repository.query.CommunityQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CommunityRepository extends JpaRepository<Community, UUID> {
    Optional<Community> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Community> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(value = CommunityQueries.DISCOVER, countQuery = CommunityQueries.DISCOVER_COUNT, nativeQuery = true)
    Page<Community> findDiscover(Pageable pageable);
}

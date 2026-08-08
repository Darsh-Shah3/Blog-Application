package com.threadly.community.repository;

import com.threadly.community.entity.CommunityMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityMessageRepository extends JpaRepository<CommunityMessage, UUID> {
    Page<CommunityMessage> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
}

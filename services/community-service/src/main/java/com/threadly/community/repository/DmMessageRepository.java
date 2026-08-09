package com.threadly.community.repository;

import com.threadly.community.entity.DmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DmMessageRepository extends JpaRepository<DmMessage, UUID> {

    Page<DmMessage> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);
}

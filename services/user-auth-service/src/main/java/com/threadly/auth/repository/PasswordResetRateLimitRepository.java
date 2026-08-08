package com.threadly.auth.repository;

import com.threadly.auth.entity.PasswordResetRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordResetRateLimitRepository extends JpaRepository<PasswordResetRateLimit, UUID> {
}

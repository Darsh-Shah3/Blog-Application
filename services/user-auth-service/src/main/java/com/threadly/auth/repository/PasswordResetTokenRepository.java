package com.threadly.auth.repository;

import com.threadly.auth.entity.PasswordResetToken;
import com.threadly.auth.repository.query.AuthQueries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(AuthQueries.PASSWORD_RESET_DELETE_UNUSED_FOR_USER)
    void deleteUnusedForUser(@Param("userId") Long userId);
}

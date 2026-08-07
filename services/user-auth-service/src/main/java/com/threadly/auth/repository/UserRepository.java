package com.threadly.auth.repository;

import com.threadly.auth.entity.User;
import com.threadly.auth.repository.query.AuthQueries;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(AuthQueries.USER_FIND_ACTIVE_BY_EMAIL)
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query(AuthQueries.USER_FIND_ACTIVE_BY_USERNAME)
    Optional<User> findActiveByUsername(@Param("username") String username);

    @Query(AuthQueries.USER_EXISTS_ACTIVE_BY_EMAIL)
    boolean existsActiveByEmail(@Param("email") String email);

    @Query(AuthQueries.USER_EXISTS_ACTIVE_BY_USERNAME)
    boolean existsActiveByUsername(@Param("username") String username);

    @Query(AuthQueries.USER_FIND_ALL_ACTIVE)
    Page<User> findAllActive(Pageable pageable);

    @Query(AuthQueries.USER_SEARCH_ACTIVE)
    Page<User> searchActive(@Param("term") String term, Pageable pageable);

    @Query(AuthQueries.USER_COUNT_ACTIVE)
    long countActive();

    @Query(AuthQueries.USER_COUNT_ACTIVE_BY_ROLE_NAME)
    long countActiveByRoleName(@Param("roleName") String roleName);
}

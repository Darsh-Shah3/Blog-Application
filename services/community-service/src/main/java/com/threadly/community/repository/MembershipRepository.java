package com.threadly.community.repository;

import com.threadly.community.entity.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    boolean existsByCommunityIdAndUserId(UUID communityId, UUID userId);

    List<Membership> findByUserId(UUID userId);

    Page<Membership> findByUserId(UUID userId, Pageable pageable);

    List<Membership> findByCommunityId(UUID communityId);

    Page<Membership> findByCommunityId(UUID communityId, Pageable pageable);

    void deleteByCommunityId(UUID communityId);

    @Query("select m.communityId from Membership m where m.userId = :userId and m.communityId in :communityIds")
    Set<UUID> findJoinedCommunityIds(
            @Param("userId") UUID userId,
            @Param("communityIds") Collection<UUID> communityIds);
}

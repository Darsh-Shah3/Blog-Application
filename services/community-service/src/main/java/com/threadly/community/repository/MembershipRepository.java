package com.threadly.community.repository;

import com.threadly.community.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByCommunityIdAndUserId(Long communityId, Long userId);
    boolean existsByCommunityIdAndUserId(Long communityId, Long userId);
    List<Membership> findByUserId(Long userId);
    List<Membership> findByCommunityId(Long communityId);
    void deleteByCommunityId(Long communityId);
}

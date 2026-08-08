package com.threadly.community.config;

import com.threadly.community.entity.Community;
import com.threadly.community.entity.Membership;
import com.threadly.community.repository.CommunityRepository;
import com.threadly.community.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Optional Java seed (disabled by default). Prefer {@code scripts/seed-demo-data.sql}.
 */
@Component
@Order(30)
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoCommunitySeed implements ApplicationRunner {

    public static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    public static final UUID COMMUNITY_PROGRAMMING = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    public static final UUID COMMUNITY_DESIGN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    public static final UUID COMMUNITY_CAREER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");

    private static final Logger log = LoggerFactory.getLogger(DemoCommunitySeed.class);

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int added = 0;
        added += seedIfMissing(COMMUNITY_PROGRAMMING, "Programming", "programming",
                "Code, systems, languages, and shipping software.");
        added += seedIfMissing(COMMUNITY_DESIGN, "Design Systems", "design-systems",
                "UI craft, product design, and design-engineering collaboration.");
        added += seedIfMissing(COMMUNITY_CAREER, "Career Growth", "career-growth",
                "Interviews, portfolios, promotions, and learning in public.");
        if (added > 0) {
            log.info("Seeded {} demo communities", added);
        }
    }

    private int seedIfMissing(UUID id, String name, String slug, String description) {
        if (communityRepository.existsBySlug(slug) || communityRepository.existsById(id)) {
            return 0;
        }
        Map<String, Object> creator = new LinkedHashMap<>();
        creator.put("id", DEMO_USER_ID.toString());
        creator.put("username", "demo");

        Community c = Community.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .description(description)
                .creator(creator)
                .creatorId(DEMO_USER_ID)
                .memberCount(1L)
                .createdBy("seed")
                .updatedBy("seed")
                .build();
        // Flush first so membership FK can see the community row in the same transaction.
        c = communityRepository.saveAndFlush(c);
        UUID communityId = c.getId();

        Map<String, Object> communitySnap = new LinkedHashMap<>();
        communitySnap.put("id", communityId.toString());
        communitySnap.put("name", name);
        communitySnap.put("slug", slug);

        Map<String, Object> memberSnap = new LinkedHashMap<>();
        memberSnap.put("id", DEMO_USER_ID.toString());
        memberSnap.put("username", "demo");

        membershipRepository.save(Membership.builder()
                .communityId(communityId)
                .community(communitySnap)
                .userId(DEMO_USER_ID)
                .member(memberSnap)
                .build());
        return 1;
    }
}

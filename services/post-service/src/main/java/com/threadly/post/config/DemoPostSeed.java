package com.threadly.post.config;

import com.threadly.post.entity.Post;
import com.threadly.post.model.NamedRef;
import com.threadly.post.model.UserRef;
import com.threadly.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Optional Java seed (disabled by default). Prefer {@code scripts/seed-demo-data.sql}.
 */
@Component
@Order(30)
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoPostSeed implements ApplicationRunner {

    private static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID ALICE_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final UUID COMMUNITY_PROGRAMMING = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID COMMUNITY_DESIGN = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID COMMUNITY_CAREER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");

    private static final Logger log = LoggerFactory.getLogger(DemoPostSeed.class);

    private final PostRepository postRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (postRepository.count() >= 3) {
            return;
        }
        Instant now = Instant.now();
        save(seed(COMMUNITY_PROGRAMMING, "Programming", "programming", DEMO_USER_ID, "demo",
                "Welcome to Threadly — microservices demo feed",
                "Browse as a guest, sign in to post, vote, join, and schedule publications. "
                        + "This content is seeded into Postgres so every fresh environment has sample data.",
                12L, 3L, now.minus(2, ChronoUnit.HOURS)));
        save(seed(COMMUNITY_PROGRAMMING, "Programming", "programming", ALICE_USER_ID, "alice",
                "Spring Boot + Next.js stack walkthrough",
                "API gateway, JWT, Flyway, and Docker Compose — a clean path from local to demo.",
                28L, 5L, now.minus(6, ChronoUnit.HOURS)));
        save(seed(COMMUNITY_DESIGN, "Design Systems", "design-systems", DEMO_USER_ID, "demo",
                "Calm UI without the default AI look",
                "Dark chrome, serif headings, and restrained orange accents so the product feels intentional.",
                19L, 2L, now.minus(1, ChronoUnit.DAYS)));
        save(seed(COMMUNITY_CAREER, "Career Growth", "career-growth", ALICE_USER_ID, "alice",
                "How I explain this project in interviews",
                "Lead with user problems (communities, feed, auth), then architecture (bounded services, events), "
                        + "then trade-offs (consistency vs ownership).",
                41L, 8L, now.minus(30, ChronoUnit.MINUTES)));
        save(seed(COMMUNITY_DESIGN, "Design Systems", "design-systems", ALICE_USER_ID, "alice",
                "LinkedIn-style composer vs Reddit submit form",
                "Same APIs underneath; the composer makes media, schedule, and community feel obvious.",
                9L, 1L, now.minus(3, ChronoUnit.DAYS)));
        log.info("Seeded demo posts (count was under 3)");
    }

    private static Post seed(
            UUID communityId,
            String communityName,
            String slug,
            UUID authorId,
            String authorUsername,
            String title,
            String content,
            long score,
            long comments,
            Instant createdAt
    ) {
        return Post.builder()
                .communityId(communityId)
                .community(NamedRef.builder().id(communityId).name(communityName).slug(slug).build())
                .authorId(authorId)
                .author(UserRef.builder().id(authorId).username(authorUsername).build())
                .title(title)
                .content(content)
                .postType(Post.PostType.TEXT)
                .linkUrls(List.of())
                .media(List.of())
                .score(score)
                .commentCount(comments)
                .status(Post.Status.PUBLISHED)
                .createdBy("seed")
                .updatedBy("seed")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private void save(Post p) {
        postRepository.save(p);
    }
}

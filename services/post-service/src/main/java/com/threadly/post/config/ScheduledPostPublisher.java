package com.threadly.post.config;

import com.threadly.post.cache.RedisJsonCache;
import com.threadly.post.entity.Post;
import com.threadly.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledPostPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostPublisher.class);

    private final PostRepository postRepository;
    private final ObjectProvider<RedisJsonCache> cacheProvider;

    @Value("${app.cache.redis-enabled:false}")
    private boolean cacheEnabled;

    @Scheduled(fixedDelayString = "${app.posts.schedule-publish-ms:60000}")
    @Transactional
    public void publishDue() {
        Instant now = Instant.now();
        List<Post> due = postRepository.findByStatusAndScheduledAtLessThanEqual(Post.Status.SCHEDULED, now);
        if (due.isEmpty()) {
            return;
        }
        for (Post p : due) {
            p.setStatus(Post.Status.PUBLISHED);
            p.setUpdatedAt(now);
        }
        postRepository.saveAll(due);
        if (cacheEnabled) {
            RedisJsonCache cache = cacheProvider.getIfAvailable();
            if (cache != null) {
                cache.deleteByPrefix("post:feed:v1:");
            }
        }
        log.info("Published {} scheduled posts", due.size());
    }
}

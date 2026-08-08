package com.threadly.auth.config;

import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.entity.UserFollow;
import com.threadly.auth.repository.RoleRepository;
import com.threadly.auth.repository.UserFollowRepository;
import com.threadly.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Optional Java seed (disabled by default). Prefer {@code scripts/seed-demo-data.sql}.
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoUserSeed implements ApplicationRunner {

    public static final UUID DEMO_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    public static final UUID ALICE_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeed.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserFollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));
        ensureUser(DEMO_USER_ID, "demo", "demo@threadly.local", "demo1234", "Demo User", userRole);
        ensureUser(ALICE_USER_ID, "alice", "alice@threadly.local", "alice1234", "Alice Example", userRole);
    }

    private void ensureUser(UUID id, String username, String email, String password, String display, Role userRole) {
        if (userRepository.existsActiveByUsername(username) || userRepository.existsActiveByEmail(email)) {
            return;
        }
        User u = User.builder()
                .id(id)
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(display)
                .bio("Demo account seeded for Threadly")
                .karma(42L)
                .createdBy("seed")
                .updatedBy("seed")
                .build();
        u.addRole(userRole);
        u = userRepository.save(u);
        followRepository.save(UserFollow.builder()
                .userId(u.getId())
                .username(u.getUsername())
                .build());
        log.info("Seeded demo user username={} id={}", username, id);
    }
}

package com.threadly.auth.config;

import com.threadly.auth.entity.Role;
import com.threadly.auth.entity.User;
import com.threadly.auth.repository.RoleRepository;
import com.threadly.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a platform admin on first boot so operators can assign roles without SQL.
 * Password only applied when the user is newly created (never overwritten).
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminBootstrapProperties props;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.isEnabled()) {
            return;
        }
        String username = props.getUsername().toLowerCase().trim();
        if (userRepository.existsActiveByUsername(username) || userRepository.existsActiveByEmail(props.getEmail().toLowerCase())) {
            log.debug("Bootstrap admin already present username={}", username);
            return;
        }
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing — Flyway migrations not applied?"));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));

        User admin = User.builder()
                .username(username)
                .email(props.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(props.getPassword()))
                .displayName(props.getDisplayName())
                .bio("Default platform administrator")
                .karma(0L)
                .createdBy("system")
                .updatedBy("system")
                .build();
        admin.getRoles().add(userRole);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);
        log.warn("Bootstrap admin created username={} — change the password in production", username);
    }
}

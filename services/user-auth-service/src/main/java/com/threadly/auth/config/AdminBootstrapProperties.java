package com.threadly.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public class AdminBootstrapProperties {
    /** When true, ensure a default admin account exists on startup. */
    private boolean enabled = true;
    private String username = "admin";
    private String email = "admin@threadly.local";
    private String password = "Admin@12345";
    private String displayName = "Platform Admin";
}

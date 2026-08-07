package com.threadly.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppSecurityProperties.class,
        AdminBootstrapProperties.class,
        MailAppProperties.class
})
public class AppConfig {
}

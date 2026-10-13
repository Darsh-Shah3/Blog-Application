package com.threadly.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Placeholder for aggregated OpenAPI once springdoc-webflux is on the gateway classpath.
 * Canonical pom has no springdoc yet (Phase 1 locks down; aggregation is later).
 * Kept profile-gated so prod never loads Swagger-related gateway config.
 */
@Configuration
@Profile("!prod")
@ConditionalOnClass(name = "org.springdoc.core.configuration.SpringDocConfiguration")
public class OpenApiConfig {
    // OpenAPI @Bean restored when springdoc-openapi-starter-webflux-ui is added to api-gateway/pom.xml.
}

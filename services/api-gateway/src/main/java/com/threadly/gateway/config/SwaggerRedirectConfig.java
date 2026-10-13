package com.threadly.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

/**
 * Gateway has no HTML home page. Send browsers to the aggregated Swagger UI
 * (WebFlux springdoc serves UI under /webjars/swagger-ui/, not /swagger-ui/index.html).
 * Disabled in prod — Swagger must not be the public landing page.
 */
@Configuration
@Profile("!prod")
public class SwaggerRedirectConfig {

    private static final URI SWAGGER_UI = URI.create("/webjars/swagger-ui/index.html");

    @Bean
    public RouterFunction<ServerResponse> swaggerRedirects() {
        return RouterFunctions.route()
                .GET("/", req -> ServerResponse.status(HttpStatus.FOUND).location(SWAGGER_UI).build())
                .GET("/swagger-ui/index.html", req -> ServerResponse.status(HttpStatus.FOUND).location(SWAGGER_UI).build())
                .build();
    }
}

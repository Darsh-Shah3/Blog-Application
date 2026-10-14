package com.threadly.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Gateway has no aggregated SpringDoc UI on the classpath.
 * Local debug: send browsers to a small index of per-service Swagger links
 * (and correct common path typos like swagger_ui).
 * Disabled in prod.
 */
@Configuration
@Profile("!prod")
public class SwaggerRedirectConfig {

    private static final String INDEX_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>Threadly API docs (local)</title>
          <style>
            :root { color-scheme: dark light; font-family: system-ui, sans-serif; }
            body { max-width: 42rem; margin: 2.5rem auto; padding: 0 1.25rem; line-height: 1.5; }
            h1 { font-size: 1.45rem; margin-bottom: 0.35rem; }
            p { color: #8b949e; }
            ul { padding-left: 1.1rem; }
            li { margin: 0.45rem 0; }
            a { color: #3ecfc2; }
            code { background: rgba(127,127,127,.15); padding: 0.1rem 0.35rem; border-radius: 4px; }
            .note { margin-top: 1.5rem; padding: 0.85rem 1rem; border: 1px solid #30363d; border-radius: 10px; }
          </style>
        </head>
        <body>
          <h1>Threadly — local Swagger</h1>
          <p>The API gateway does not host aggregated OpenAPI UI. Open each service directly:</p>
          <ul>
            <li><a href="http://localhost:8081/swagger-ui/index.html">Auth (8081)</a></li>
            <li><a href="http://localhost:8082/swagger-ui/index.html">Community (8082)</a></li>
            <li><a href="http://localhost:8083/swagger-ui/index.html">Post (8083)</a></li>
            <li><a href="http://localhost:8084/swagger-ui/index.html">Comment (8084)</a></li>
            <li><a href="http://localhost:8085/swagger-ui/index.html">Vote (8085)</a></li>
            <li><a href="http://localhost:8086/swagger-ui/index.html">Media (8086)</a></li>
            <li><a href="http://localhost:8087/swagger-ui/index.html">Audit (8087)</a></li>
            <li><a href="http://localhost:8088/swagger-ui/index.html">Notification (8088)</a></li>
          </ul>
          <div class="note">
            <strong>Tip:</strong> use <code>/swagger-ui/index.html</code>
            (hyphen), not <code>swagger_ui</code>. Start with Auth → login → copy JWT → Authorize on other UIs.
          </div>
        </body>
        </html>
        """;

    @Bean
    public RouterFunction<ServerResponse> swaggerRedirects() {
        return RouterFunctions.route()
                .GET("/", req -> swaggerIndex())
                .GET("/swagger-ui", req -> swaggerIndex())
                .GET("/swagger-ui/", req -> swaggerIndex())
                .GET("/swagger-ui.html", req -> swaggerIndex())
                .GET("/swagger-ui/index.html", req -> swaggerIndex())
                .GET("/webjars/swagger-ui/index.html", req -> swaggerIndex())
                .GET("/webjars/swagger_ui/index.html", req -> swaggerIndex())
                .GET("/webjars/swagger_ui/", req -> swaggerIndex())
                .build();
    }

    private static Mono<ServerResponse> swaggerIndex() {
        byte[] bytes = INDEX_HTML.getBytes(StandardCharsets.UTF_8);
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .body((outputMessage, context) -> {
                    DataBuffer buffer = outputMessage.bufferFactory().wrap(bytes);
                    return outputMessage.writeWith(Mono.just(buffer));
                });
    }
}

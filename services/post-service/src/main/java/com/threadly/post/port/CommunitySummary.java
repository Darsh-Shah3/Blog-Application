package com.threadly.post.port;

/** Anti-corruption DTO: only the fields post-service actually needs from community. */
public record CommunitySummary(Long id, String name, String slug) {
}

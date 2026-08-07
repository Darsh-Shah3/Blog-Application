package com.threadly.community.controller;

import com.threadly.community.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL')")
public class InternalStatsController {

    private final CommunityRepository communityRepository;

    @GetMapping("/api/v1/internal/stats/communities")
    public ResponseEntity<Map<String, Long>> communityStats() {
        return ResponseEntity.ok(Map.of("count", communityRepository.count()));
    }
}

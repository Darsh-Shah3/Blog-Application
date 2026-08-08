package com.threadly.post.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Denormalized display snapshot stored as JSONB. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedRef {
    private UUID id;
    private String name;
    private String slug;
}

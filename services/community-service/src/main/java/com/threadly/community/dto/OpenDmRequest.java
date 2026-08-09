package com.threadly.community.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class OpenDmRequest {
    @NotNull
    private UUID peerUserId;

    /** Optional display snapshot if the client already knows the peer username. */
    private String peerUsername;
}

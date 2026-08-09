package com.threadly.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100)
    private String displayName;

    @Size(max = 500)
    private String bio;

    /**
     * When present: non-blank UUID string sets the avatar; blank string clears it.
     * When omitted (null), avatar is left unchanged.
     */
    @Size(max = 40)
    private String avatarMediaId;
}

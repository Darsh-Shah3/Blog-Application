package com.threadly.community.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteMessageRequest {
    /** ME = hide for current user only; EVERYONE = remove for all participants (WhatsApp-style). */
    public enum Scope {
        ME,
        EVERYONE
    }

    @NotNull
    private Scope scope = Scope.ME;
}

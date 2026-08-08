package com.threadly.post.dto;

import jakarta.validation.constraints.NotNull;

public class CommentCountDeltaRequest {
    @NotNull
    private Long delta;

    public Long getDelta() {
        return delta;
    }

    public void setDelta(Long delta) {
        this.delta = delta;
    }
}

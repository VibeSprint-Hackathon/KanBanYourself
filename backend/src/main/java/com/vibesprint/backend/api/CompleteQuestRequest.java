package com.vibesprint.backend.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteQuestRequest(
        @NotBlank(message = "Event id must not be blank") String eventId,
        @NotNull(message = "Progression source is required") Source source
) {

    public enum Source {
        DEMO,
        GITHUB
    }
}

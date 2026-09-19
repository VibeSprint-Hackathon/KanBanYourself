package com.vibesprint.backend.api;

import com.vibesprint.backend.quest.QuestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateQuestRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Size(max = 2000) String description,
        @NotNull QuestStatus status,
        @Min(0) @Max(100) Integer progress,
        @Positive int xpReward,
        @Positive long assigneeId,
        @Size(max = 500) String externalReference
) {
}

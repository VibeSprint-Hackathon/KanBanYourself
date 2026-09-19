package com.vibesprint.backend.api;

import com.vibesprint.backend.quest.QuestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MoveQuestRequest(
        @NotNull QuestStatus status,
        @Positive Long beforeQuestId
) {
}

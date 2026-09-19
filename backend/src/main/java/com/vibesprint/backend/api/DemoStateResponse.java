package com.vibesprint.backend.api;

import java.util.List;

public record DemoStateResponse(
        PlayerView player,
        List<QuestView> quests,
        RaidView raid,
        UnlockView nextUnlock
) {

    public DemoStateResponse {
        quests = List.copyOf(quests);
    }

    public record PlayerView(
            long id,
            String name,
            int totalXp,
            int level,
            Integer nextLevelXp,
            String title,
            String cosmeticKey,
            String characterState
    ) {
    }

    public record QuestView(
            long id,
            String title,
            String description,
            String status,
            Integer progress,
            int xpReward,
            long assigneeId,
            String externalReference,
            int sortOrder
    ) {
    }

    public record RaidView(
            long id,
            String name,
            String description,
            int maxHp,
            int currentHp,
            String status,
            String externalReference
    ) {
    }

    public record UnlockView(int level, String cosmeticKey, String displayName) {
    }
}

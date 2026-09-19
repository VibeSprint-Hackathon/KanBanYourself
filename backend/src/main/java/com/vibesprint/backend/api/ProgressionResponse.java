package com.vibesprint.backend.api;

import java.util.List;

public record ProgressionResponse(
        String eventId,
        boolean applied,
        String reason,
        int xpGained,
        int raidDamage,
        boolean levelUp,
        CosmeticView unlockedCosmetic,
        String reaction,
        boolean bossDefeated,
        List<AchievementUnlockView> unlockedAchievements,
        DemoStateResponse.QuestView quest,
        DemoStateResponse.PlayerView player,
        DemoStateResponse.RaidView raid,
        String source,
        boolean duplicate
) {

    public record CosmeticView(String key, String displayName) {
    }

    public record AchievementUnlockView(
            String key,
            String name,
            String description,
            String category,
            String rewardLabel
    ) {
    }
}

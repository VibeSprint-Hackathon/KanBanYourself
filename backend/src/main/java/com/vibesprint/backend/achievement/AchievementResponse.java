package com.vibesprint.backend.achievement;

import java.time.Instant;
import java.util.List;

public record AchievementResponse(
        long playerId,
        int unlocked,
        int total,
        List<AchievementView> achievements
) {
    public record AchievementView(
            AchievementKey key,
            String name,
            String description,
            AchievementCategory category,
            int currentProgress,
            int targetProgress,
            boolean unlocked,
            Instant unlockedAt,
            String rewardLabel,
            boolean featured
    ) {
    }
}

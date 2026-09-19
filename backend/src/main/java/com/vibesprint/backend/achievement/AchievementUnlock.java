package com.vibesprint.backend.achievement;

public record AchievementUnlock(
        AchievementKey key,
        String name,
        String description,
        AchievementCategory category,
        String rewardLabel
) {
}

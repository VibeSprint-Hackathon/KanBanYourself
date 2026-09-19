package com.vibesprint.backend.achievement;

import java.util.List;

public record AchievementDefinition(
        AchievementKey key,
        String name,
        String description,
        AchievementCategory category,
        int targetProgress,
        String rewardLabel
) {
    public static final List<AchievementDefinition> CATALOG = List.of(
            definition(AchievementKey.FIRST_QUEST, "First Quest", "Complete your first Quest.", AchievementCategory.QUEST, 1, "First Steps badge"),
            definition(AchievementKey.QUEST_APPRENTICE, "Quest Apprentice", "Complete 3 Quests.", AchievementCategory.QUEST, 3, "Quest Apprentice badge"),
            definition(AchievementKey.QUEST_MASTER, "Quest Master", "Complete 5 Quests.", AchievementCategory.QUEST, 5, "Quest Master badge"),
            definition(AchievementKey.XP_HUNTER, "XP Hunter", "Earn 500 XP from new Quest completions.", AchievementCategory.XP, 500, "XP Hunter badge"),
            definition(AchievementKey.LEVEL_UP, "Level Up", "Gain a level from a Quest completion.", AchievementCategory.LEVEL, 1, "Level Up badge"),
            definition(AchievementKey.FIRST_STRIKE, "First Strike", "Deal your first Raid damage.", AchievementCategory.RAID, 1, "First Strike badge"),
            definition(AchievementKey.HEAVY_HITTER, "Heavy Hitter", "Deal 500 cumulative Raid damage.", AchievementCategory.RAID, 500, "Heavy Hitter badge"),
            definition(AchievementKey.BOSS_SLAYER, "Boss Slayer", "Deal the finishing blow to a Raid boss.", AchievementCategory.RAID, 1, "Boss Slayer badge"),
            definition(AchievementKey.GITHUB_HERO, "GitHub Hero", "Complete a Quest through GitHub.", AchievementCategory.GITHUB, 1, "GitHub Hero badge"),
            definition(AchievementKey.RAID_CONTRIBUTOR, "Raid Contributor", "Deal Raid damage in 3 Quest completions.", AchievementCategory.RAID, 3, "Raid Contributor badge")
    );

    private static AchievementDefinition definition(
            AchievementKey key,
            String name,
            String description,
            AchievementCategory category,
            int target,
            String reward
    ) {
        return new AchievementDefinition(key, name, description, category, target, reward);
    }
}

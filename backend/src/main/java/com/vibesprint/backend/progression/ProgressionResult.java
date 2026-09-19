package com.vibesprint.backend.progression;

import com.vibesprint.backend.achievement.AchievementUnlock;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.raid.RaidStatus;

import java.util.List;

public record ProgressionResult(
        String eventId,
        boolean applied,
        Reason reason,
        int xpGained,
        int raidDamage,
        boolean levelUp,
        CosmeticUnlock unlockedCosmetic,
        Reaction reaction,
        boolean bossDefeated,
        List<AchievementUnlock> unlockedAchievements,
        QuestSnapshot quest,
        PlayerSnapshot player,
        RaidSnapshot raid
) {

    public ProgressionResult {
        unlockedAchievements = List.copyOf(unlockedAchievements);
    }

    public enum Reason {
        ALREADY_COMPLETED
    }

    public enum Reaction {
        HAPPY,
        LEVEL_UP
    }

    public enum CharacterState {
        IDLE,
        CODING
    }

    public record QuestSnapshot(
            long id,
            String title,
            String description,
            QuestStatus status,
            Integer progress,
            int xpReward,
            long assigneeId,
            String externalReference,
            int sortOrder
    ) {
    }

    public record PlayerSnapshot(
            long id,
            String name,
            String githubLogin,
            int totalXp,
            int level,
            Integer nextLevelXp,
            String title,
            String cosmeticKey,
            CharacterState characterState
    ) {
    }

    public record RaidSnapshot(
            long id,
            String name,
            String description,
            int maxHp,
            int currentHp,
            RaidStatus status,
            String externalReference
    ) {
    }
}

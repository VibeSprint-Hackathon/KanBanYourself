package com.vibesprint.backend.progression;

import com.vibesprint.backend.quest.QuestStatus;

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
        QuestSnapshot quest,
        PlayerSnapshot player,
        RaidSnapshot raid
) {

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

    public enum RaidStatus {
        ACTIVE,
        DEFEATED
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
            int maxHp,
            int currentHp,
            RaidStatus status
    ) {
    }
}

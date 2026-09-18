package com.vibesprint.backend.progression;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProgressionRules {

    private static final int[] LEVEL_THRESHOLDS = {0, 200, 400, 600, 1000, 1500};
    private static final int RARE_HOODIE_XP = 1000;
    private static final CosmeticUnlock RARE_HOODIE = new CosmeticUnlock("rare-hoodie", "Rare Hoodie");

    public PlayerProgress describe(int totalXp) {
        if (totalXp < 0) {
            throw new IllegalArgumentException("Total XP must not be negative");
        }

        int levelIndex = 0;
        for (int index = 1; index < LEVEL_THRESHOLDS.length; index++) {
            if (totalXp < LEVEL_THRESHOLDS[index]) {
                break;
            }
            levelIndex = index;
        }

        int level = levelIndex + 1;
        Integer nextLevelXp = levelIndex + 1 < LEVEL_THRESHOLDS.length
                ? LEVEL_THRESHOLDS[levelIndex + 1]
                : null;
        boolean rareHoodieUnlocked = totalXp >= RARE_HOODIE_XP;

        return new PlayerProgress(
                level,
                nextLevelXp,
                rareHoodieUnlocked ? "Code Raider" : "Code Adventurer",
                rareHoodieUnlocked ? RARE_HOODIE.key() : "base"
        );
    }

    public Optional<CosmeticUnlock> unlockedCosmetic(int previousXp, int currentXp) {
        if (previousXp < 0 || currentXp < previousXp) {
            throw new IllegalArgumentException("XP progression must not decrease");
        }
        if (previousXp < RARE_HOODIE_XP && currentXp >= RARE_HOODIE_XP) {
            return Optional.of(RARE_HOODIE);
        }
        return Optional.empty();
    }

    public record PlayerProgress(int level, Integer nextLevelXp, String title, String cosmeticKey) {
    }
}

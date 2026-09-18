package com.vibesprint.backend.progression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRulesTests {

    private final ProgressionRules progressionRules = new ProgressionRules();

    @ParameterizedTest
    @CsvSource({
            "0, 1",
            "199, 1",
            "200, 2",
            "399, 2",
            "400, 3",
            "599, 3",
            "600, 4",
            "999, 4",
            "1000, 5",
            "1499, 5",
            "1500, 6"
    })
    void calculatesLevelAtEveryBoundary(int totalXp, int expectedLevel) {
        assertEquals(expectedLevel, progressionRules.describe(totalXp).level());
    }

    @Test
    void describesDemoProgressBeforeAndAfterLevelUp() {
        assertEquals(
                new ProgressionRules.PlayerProgress(4, 1000, "Code Adventurer", "base"),
                progressionRules.describe(920)
        );
        assertEquals(
                new ProgressionRules.PlayerProgress(5, 1500, "Code Raider", "rare-hoodie"),
                progressionRules.describe(1100)
        );
    }

    @Test
    void unlocksRareHoodieOnlyWhenThresholdIsCrossed() {
        assertEquals(
                new CosmeticUnlock("rare-hoodie", "Rare Hoodie"),
                progressionRules.unlockedCosmetic(920, 1100).orElseThrow()
        );
        assertFalse(progressionRules.unlockedCosmetic(1000, 1180).isPresent());
        assertFalse(progressionRules.unlockedCosmetic(920, 999).isPresent());
    }

    @Test
    void describesOnlyRemainingMvpUnlock() {
        assertEquals(
                new ProgressionRules.UnlockTarget(5, new CosmeticUnlock("rare-hoodie", "Rare Hoodie")),
                progressionRules.nextUnlock(920).orElseThrow()
        );
        assertFalse(progressionRules.nextUnlock(1000).isPresent());
    }

    @Test
    void rejectsInvalidProgressionValues() {
        assertThrows(IllegalArgumentException.class, () -> progressionRules.describe(-1));
        assertThrows(IllegalArgumentException.class, () -> progressionRules.unlockedCosmetic(100, 99));
    }

    @Test
    void validatesCompletionCommand() {
        assertThrows(
                InvalidProgressionCommandException.class,
                () -> new CompleteQuestCommand(0, "event", ProgressionSource.DEMO)
        );
        assertThrows(
                InvalidProgressionCommandException.class,
                () -> new CompleteQuestCommand(101, " ", ProgressionSource.DEMO)
        );
        assertThrows(
                InvalidProgressionCommandException.class,
                () -> new CompleteQuestCommand(101, "event", null)
        );
        assertTrue(new CompleteQuestCommand(101, "event", ProgressionSource.DEMO).questId() > 0);
    }
}

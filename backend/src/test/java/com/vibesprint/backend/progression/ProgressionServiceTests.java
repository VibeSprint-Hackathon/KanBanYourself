package com.vibesprint.backend.progression;

import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.raid.RaidRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ProgressionServiceTests {

    @Autowired
    private ProgressionService progressionService;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private RaidRepository raidRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void completesDemoQuestAndAppliesAllProgress() {
        ProgressionResult result = progressionService.completeQuest(command(101L, "complete-101"));

        assertTrue(result.applied());
        assertNull(result.reason());
        assertEquals(180, result.xpGained());
        assertEquals(180, result.raidDamage());
        assertTrue(result.levelUp());
        assertEquals(new CosmeticUnlock("rare-hoodie", "Rare Hoodie"), result.unlockedCosmetic());
        assertEquals(ProgressionResult.Reaction.LEVEL_UP, result.reaction());
        assertTrue(result.bossDefeated());

        assertEquals(QuestStatus.DONE, result.quest().status());
        assertEquals(1100, result.player().totalXp());
        assertEquals(5, result.player().level());
        assertEquals(1500, result.player().nextLevelXp());
        assertEquals("Code Raider", result.player().title());
        assertEquals("rare-hoodie", result.player().cosmeticKey());
        assertEquals(ProgressionResult.CharacterState.IDLE, result.player().characterState());
        assertEquals(0, result.raid().currentHp());
        assertEquals(ProgressionResult.RaidStatus.DEFEATED, result.raid().status());

        assertEquals(QuestStatus.DONE, questRepository.findById(101L).orElseThrow().getStatus());
        assertEquals(1100, playerRepository.findById(1L).orElseThrow().getTotalXp());
        assertEquals(0, raidRepository.findById(201L).orElseThrow().getCurrentHp());
    }

    @Test
    void returnsCurrentStateWithoutRewardsForCompletedQuest() {
        progressionService.completeQuest(command(101L, "first-event"));
        ProgressionResult repeated = progressionService.completeQuest(command(101L, "repeated-event"));

        assertEquals("repeated-event", repeated.eventId());
        assertFalse(repeated.applied());
        assertEquals(ProgressionResult.Reason.ALREADY_COMPLETED, repeated.reason());
        assertEquals(0, repeated.xpGained());
        assertEquals(0, repeated.raidDamage());
        assertFalse(repeated.levelUp());
        assertNull(repeated.unlockedCosmetic());
        assertNull(repeated.reaction());
        assertTrue(repeated.bossDefeated());
        assertEquals(QuestStatus.DONE, repeated.quest().status());
        assertEquals(1100, repeated.player().totalXp());
        assertEquals(0, repeated.raid().currentHp());
    }

    @Test
    void allowsCompletingTodoQuest() {
        ProgressionResult result = progressionService.completeQuest(command(102L, "complete-102"));

        assertTrue(result.applied());
        assertEquals(120, result.xpGained());
        assertEquals(1040, result.player().totalXp());
        assertEquals(5, result.player().level());
        assertEquals(60, result.raid().currentHp());
        assertFalse(result.bossDefeated());
        assertEquals(ProgressionResult.CharacterState.CODING, result.player().characterState());
    }

    @Test
    void returnsHappyReactionAndClampsOverkillDamage() {
        jdbcTemplate.update("update player set total_xp = 700 where id = 1");
        jdbcTemplate.update("update raid set current_hp = 50 where id = 201");

        ProgressionResult result = progressionService.completeQuest(command(102L, "overkill-102"));

        assertTrue(result.applied());
        assertEquals(120, result.xpGained());
        assertEquals(120, result.raidDamage());
        assertFalse(result.levelUp());
        assertNull(result.unlockedCosmetic());
        assertEquals(ProgressionResult.Reaction.HAPPY, result.reaction());
        assertEquals(0, result.raid().currentHp());
        assertTrue(result.bossDefeated());
    }

    @Test
    void rejectsUnknownQuest() {
        QuestNotFoundException exception = assertThrows(
                QuestNotFoundException.class,
                () -> progressionService.completeQuest(command(999L, "missing-quest"))
        );

        assertEquals(999L, exception.getQuestId());
    }

    @Test
    void rejectsIncompleteDemoStateBeforeMutation() {
        raidRepository.deleteAllInBatch();

        assertThrows(
                DemoStateNotReadyException.class,
                () -> progressionService.completeQuest(command(101L, "missing-raid"))
        );
        assertEquals(QuestStatus.IN_PROGRESS, questRepository.findById(101L).orElseThrow().getStatus());
        assertEquals(920, playerRepository.findById(1L).orElseThrow().getTotalXp());
    }

    @Test
    void rejectsNullCommand() {
        assertThrows(InvalidProgressionCommandException.class, () -> progressionService.completeQuest(null));
    }

    private CompleteQuestCommand command(long questId, String eventId) {
        return new CompleteQuestCommand(questId, eventId, ProgressionSource.DEMO);
    }
}

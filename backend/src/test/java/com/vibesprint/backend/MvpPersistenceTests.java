package com.vibesprint.backend;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.integration.DemoResetService;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.raid.Raid;
import com.vibesprint.backend.raid.RaidRepository;
import com.vibesprint.backend.raid.RaidStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MvpPersistenceTests {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private RaidRepository raidRepository;

    @Autowired
    private DemoResetService resetService;

    @BeforeEach
    void resetDemo() {
        resetService.reset();
    }

    @Test
    void readsDeterministicMvpSeed() {
        Player player = playerRepository.findById(1L).orElseThrow();
        assertEquals("Andrei", player.getName());
        assertEquals(920, player.getTotalXp());
        assertEquals("amjastsov", player.getGithubLogin());
        assertEquals(3, playerRepository.count());

        List<Quest> quests = questRepository.findAllInBoardOrder();
        assertEquals(
                List.of(104L, 105L, 102L, 106L, 101L, 107L, 108L, 109L, 103L, 110L),
                quests.stream().map(Quest::getId).toList()
        );

        Quest activeQuest = questRepository.findById(101L).orElseThrow();
        assertEquals("Fix payment validation", activeQuest.getTitle());
        assertEquals("Fix server-side validation and cover the payment edge cases.", activeQuest.getDescription());
        assertEquals(QuestStatus.IN_PROGRESS, activeQuest.getStatus());
        assertEquals(72, activeQuest.getProgress());
        assertEquals(180, activeQuest.getXpReward());
        assertEquals(1L, activeQuest.getAssignee().getId());
        assertNull(activeQuest.getExternalReference());
        assertEquals(100, activeQuest.getSortOrder());

        Raid raid = raidRepository.findByStatus(RaidStatus.ACTIVE).orElseThrow();
        assertEquals(201L, raid.getId());
        assertEquals("Merge Conflict Hydra", raid.getName());
        assertEquals(1000, raid.getMaxHp());
        assertEquals(180, raid.getCurrentHp());
        assertEquals("Defeat the merge conflicts blocking the team sprint.", raid.getDescription());
        assertEquals(RaidStatus.ACTIVE, raid.getStatus());
    }

    @Test
    void locksQuestInsideTransaction() {
        assertTrue(questRepository.findByIdForUpdate(101L).isPresent());
    }
}

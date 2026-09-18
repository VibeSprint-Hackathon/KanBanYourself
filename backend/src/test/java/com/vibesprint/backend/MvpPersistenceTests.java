package com.vibesprint.backend;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.raid.Raid;
import com.vibesprint.backend.raid.RaidRepository;
import org.junit.jupiter.api.Test;
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

    @Test
    void readsDeterministicMvpSeed() {
        Player player = playerRepository.findById(1L).orElseThrow();
        assertEquals("Andrei", player.getName());
        assertEquals(920, player.getTotalXp());

        List<Quest> quests = questRepository.findAllByOrderByIdAsc();
        assertEquals(List.of(101L, 102L, 103L), quests.stream().map(Quest::getId).toList());

        Quest activeQuest = quests.get(0);
        assertEquals("Fix payment validation", activeQuest.getTitle());
        assertEquals(QuestStatus.IN_PROGRESS, activeQuest.getStatus());
        assertEquals(180, activeQuest.getXpReward());
        assertEquals(1L, activeQuest.getAssignee().getId());
        assertNull(activeQuest.getExternalReference());

        Raid raid = raidRepository.findFirstByOrderByIdAsc().orElseThrow();
        assertEquals(201L, raid.getId());
        assertEquals("Merge Conflict Hydra", raid.getName());
        assertEquals(1000, raid.getMaxHp());
        assertEquals(180, raid.getCurrentHp());
    }

    @Test
    void locksQuestInsideTransaction() {
        assertTrue(questRepository.findByIdForUpdate(101L).isPresent());
    }
}

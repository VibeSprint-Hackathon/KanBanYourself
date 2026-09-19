package com.vibesprint.backend.achievement;

import com.vibesprint.backend.integration.DemoResetService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AchievementApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoResetService resetService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetDemo() {
        resetService.reset();
    }

    @Test
    void startsEveryPlayerWithTenLockedAchievements() throws Exception {
        for (long playerId = 1; playerId <= 3; playerId++) {
            mockMvc.perform(get("/api/players/{playerId}/achievements", playerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.playerId").value(playerId))
                    .andExpect(jsonPath("$.unlocked").value(0))
                    .andExpect(jsonPath("$.total").value(10))
                    .andExpect(jsonPath("$.achievements", hasSize(10)))
                    .andExpect(jsonPath("$.achievements[*].currentProgress", containsInAnyOrder(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        }
    }

    @Test
    void completionPersistsProgressAndReturnsAllNewUnlocks() throws Exception {
        complete(101, "first-andrei", "DEMO")
                .andExpect(jsonPath("$.unlockedAchievements[*].key", containsInAnyOrder(
                        "FIRST_QUEST", "LEVEL_UP", "FIRST_STRIKE", "BOSS_SLAYER"
                )));

        mockMvc.perform(get("/api/players/1/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.achievements[0].unlocked").value(true))
                .andExpect(jsonPath("$.achievements[1].currentProgress").value(1))
                .andExpect(jsonPath("$.achievements[2].currentProgress").value(1))
                .andExpect(jsonPath("$.achievements[3].currentProgress").value(180))
                .andExpect(jsonPath("$.achievements[7].unlocked").value(true));

        complete(101, "duplicate-andrei", "DEMO")
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.unlockedAchievements", empty()));

        mockMvc.perform(get("/api/players/1/achievements"))
                .andExpect(jsonPath("$.achievements[1].currentProgress").value(1))
                .andExpect(jsonPath("$.achievements[3].currentProgress").value(180));
    }

    @Test
    void accumulatesAndCapsCountersAcrossCompletions() throws Exception {
        jdbcTemplate.update("update raid set max_hp = 2000, current_hp = 2000 where id = 201");
        entityManager.clear();

        complete(101, "andrei-1", "DEMO");
        complete(106, "andrei-2", "DEMO");
        complete(109, "andrei-3", "DEMO");

        mockMvc.perform(get("/api/players/1/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.achievements[1].currentProgress").value(3))
                .andExpect(jsonPath("$.achievements[1].unlocked").value(true))
                .andExpect(jsonPath("$.achievements[2].currentProgress").value(3))
                .andExpect(jsonPath("$.achievements[3].currentProgress").value(500))
                .andExpect(jsonPath("$.achievements[6].currentProgress").value(500))
                .andExpect(jsonPath("$.achievements[9].currentProgress").value(3));
    }

    @Test
    void isolatesPlayersAndUsesExplicitGithubSource() throws Exception {
        complete(102, "timofei-github", "GITHUB")
                .andExpect(jsonPath("$.unlockedAchievements[*].key", containsInAnyOrder(
                        "FIRST_QUEST", "FIRST_STRIKE", "GITHUB_HERO"
                )));

        mockMvc.perform(get("/api/players/1/achievements"))
                .andExpect(jsonPath("$.unlocked").value(0));
        mockMvc.perform(get("/api/players/2/achievements"))
                .andExpect(jsonPath("$.achievements[0].unlocked").value(true))
                .andExpect(jsonPath("$.achievements[8].unlocked").value(true));
        mockMvc.perform(get("/api/players/3/achievements"))
                .andExpect(jsonPath("$.unlocked").value(0));
    }

    @Test
    void administrativeRaidCompletionDoesNotUnlockBossSlayer() throws Exception {
        mockMvc.perform(post("/api/raids/201/complete")).andExpect(status().isOk());
        mockMvc.perform(get("/api/players/1/achievements"))
                .andExpect(jsonPath("$.achievements[7].unlocked").value(false));
    }

    @Test
    void resetRemovesPersistedAchievementState() throws Exception {
        complete(101, "before-reset", "DEMO");
        mockMvc.perform(post("/api/demo/reset")).andExpect(status().isOk());
        mockMvc.perform(get("/api/players/1/achievements"))
                .andExpect(jsonPath("$.unlocked").value(0))
                .andExpect(jsonPath("$.achievements[0].currentProgress").value(0));
    }

    @Test
    void unknownPlayerUsesExistingErrorContract() throws Exception {
        mockMvc.perform(get("/api/players/999/achievements"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.ResultActions complete(long questId, String eventId, String source)
            throws Exception {
        return mockMvc.perform(post("/api/demo/quests/{questId}/complete", questId)
                        .contentType("application/json")
                        .content("""
                                {"eventId":"%s","source":"%s"}
                                """.formatted(eventId, source)))
                .andExpect(status().isOk());
    }
}

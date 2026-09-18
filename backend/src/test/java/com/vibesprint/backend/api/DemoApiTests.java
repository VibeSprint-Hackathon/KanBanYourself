package com.vibesprint.backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemoApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsDeterministicDemoState() throws Exception {
        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.player.id").value(1))
                .andExpect(jsonPath("$.player.totalXp").value(920))
                .andExpect(jsonPath("$.player.level").value(4))
                .andExpect(jsonPath("$.player.characterState").value("coding"))
                .andExpect(jsonPath("$.quests", hasSize(3)))
                .andExpect(jsonPath("$.quests[0].id").value(101))
                .andExpect(jsonPath("$.quests[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.quests[0].externalReference").value(nullValue()))
                .andExpect(jsonPath("$.raid.id").value(201))
                .andExpect(jsonPath("$.raid.currentHp").value(180))
                .andExpect(jsonPath("$.raid.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextUnlock.level").value(5))
                .andExpect(jsonPath("$.nextUnlock.cosmeticKey").value("rare-hoodie"));
    }

    @Test
    void completesQuestReturnsContractAndPersistsForNextRead() throws Exception {
        String body = """
                {"eventId":"demo-payment-validation-1","source":"DEMO"}
                """;

        mockMvc.perform(post("/api/demo/quests/101/complete")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("demo-payment-validation-1"))
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.reason").value(nullValue()))
                .andExpect(jsonPath("$.xpGained").value(180))
                .andExpect(jsonPath("$.raidDamage").value(180))
                .andExpect(jsonPath("$.levelUp").value(true))
                .andExpect(jsonPath("$.unlockedCosmetic.key").value("rare-hoodie"))
                .andExpect(jsonPath("$.reaction").value("level-up"))
                .andExpect(jsonPath("$.bossDefeated").value(true))
                .andExpect(jsonPath("$.quest.status").value("DONE"))
                .andExpect(jsonPath("$.quest.externalReference").value(nullValue()))
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.player.characterState").value("idle"))
                .andExpect(jsonPath("$.raid.status").value("DEFEATED"));

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.quests[0].status").value("DONE"))
                .andExpect(jsonPath("$.raid.currentHp").value(0))
                .andExpect(jsonPath("$.nextUnlock").value(nullValue()));
    }

    @Test
    void returnsFullCurrentStateForRepeatedCompletion() throws Exception {
        String first = """
                {"eventId":"first-event","source":"DEMO"}
                """;
        String repeated = """
                {"eventId":"repeated-event","source":"DEMO"}
                """;

        mockMvc.perform(post("/api/demo/quests/101/complete")
                        .contentType("application/json")
                        .content(first))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/demo/quests/101/complete")
                        .contentType("application/json")
                        .content(repeated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("repeated-event"))
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.reason").value("ALREADY_COMPLETED"))
                .andExpect(jsonPath("$.xpGained").value(0))
                .andExpect(jsonPath("$.raidDamage").value(0))
                .andExpect(jsonPath("$.reaction").value(nullValue()))
                .andExpect(jsonPath("$.quest.status").value("DONE"))
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.raid.currentHp").value(0));
    }

    @Test
    void mapsInvalidRequestsToStableError() throws Exception {
        mockMvc.perform(post("/api/demo/quests/0/complete")
                        .contentType("application/json")
                        .content("{\"eventId\":\"event\",\"source\":\"DEMO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/demo/quests/101/complete")
                        .contentType("application/json")
                        .content("{\"eventId\":\" \",\"source\":\"DEMO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/demo/quests/101/complete")
                        .contentType("application/json")
                        .content("{\"eventId\":\"event\",\"source\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void mapsMissingQuestToNotFound() throws Exception {
        mockMvc.perform(post("/api/demo/quests/999/complete")
                        .contentType("application/json")
                        .content("{\"eventId\":\"missing\",\"source\":\"DEMO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUEST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Quest 999 was not found"));
    }

    @Test
    void mapsIncompleteDemoStateToConflict() throws Exception {
        jdbcTemplate.update("delete from raid");

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEMO_STATE_NOT_READY"));
    }

    @Test
    void mapsMissingPlayerToConflict() throws Exception {
        jdbcTemplate.update("delete from quest");
        jdbcTemplate.update("delete from player");

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEMO_STATE_NOT_READY"));
    }
}

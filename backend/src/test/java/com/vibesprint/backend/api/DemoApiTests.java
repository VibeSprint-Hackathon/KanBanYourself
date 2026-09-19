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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(jsonPath("$.quests", hasSize(10)))
                .andExpect(jsonPath("$.quests[0].id").value(104))
                .andExpect(jsonPath("$.quests[1].id").value(105))
                .andExpect(jsonPath("$.quests[2].id").value(102))
                .andExpect(jsonPath("$.quests[4].id").value(101))
                .andExpect(jsonPath("$.quests[4].description")
                        .value("Fix server-side validation and cover the payment edge cases."))
                .andExpect(jsonPath("$.quests[4].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.quests[4].progress").value(72))
                .andExpect(jsonPath("$.quests[4].xpReward").value(180))
                .andExpect(jsonPath("$.quests[4].assigneeId").value(1))
                .andExpect(jsonPath("$.quests[4].externalReference").value(nullValue()))
                .andExpect(jsonPath("$.quests[4].sortOrder").value(100))
                .andExpect(jsonPath("$.quests[8].id").value(103))
                .andExpect(jsonPath("$.quests[9].id").value(110))
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
                .andExpect(jsonPath("$.quest.description")
                        .value("Fix server-side validation and cover the payment edge cases."))
                .andExpect(jsonPath("$.quest.progress").value(100))
                .andExpect(jsonPath("$.quest.externalReference").value(nullValue()))
                .andExpect(jsonPath("$.quest.sortOrder").value(300))
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.player.characterState").value("idle"))
                .andExpect(jsonPath("$.raid.status").value("DEFEATED"));

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.quests[9].id").value(101))
                .andExpect(jsonPath("$.quests[9].status").value("DONE"))
                .andExpect(jsonPath("$.quests[9].progress").value(100))
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
                .andExpect(jsonPath("$.quest.progress").value(100))
                .andExpect(jsonPath("$.quest.sortOrder").value(300))
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.raid.currentHp").value(0));
    }

    @Test
    void resetsCompletedDemoToDeterministicSeed() throws Exception {
        mockMvc.perform(post("/api/demo/quests/101/complete")
                        .contentType("application/json")
                        .content("{\"eventId\":\"before-reset\",\"source\":\"DEMO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player.totalXp").value(920))
                .andExpect(jsonPath("$.player.characterState").value("coding"))
                .andExpect(jsonPath("$.quests", hasSize(10)))
                .andExpect(jsonPath("$.quests[0].id").value(104))
                .andExpect(jsonPath("$.quests[2].id").value(102))
                .andExpect(jsonPath("$.quests[4].id").value(101))
                .andExpect(jsonPath("$.quests[4].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.quests[4].progress").value(72))
                .andExpect(jsonPath("$.quests[9].id").value(110))
                .andExpect(jsonPath("$.raid.currentHp").value(180))
                .andExpect(jsonPath("$.raid.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextUnlock.cosmeticKey").value("rare-hoodie"));
    }

    @Test
    void resetRecoversMissingAndRemovesUnexpectedDemoData() throws Exception {
        jdbcTemplate.update("delete from raid");
        jdbcTemplate.update(
                "insert into raid (id, name, max_hp, current_hp) values (999, 'Unexpected Raid', 10, 10)"
        );

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raid.id").value(201))
                .andExpect(jsonPath("$.raid.currentHp").value(180));

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raid.id").value(201));
    }

    @Test
    void closesLinkedGithubIssueAndAppliesQuestProgress() throws Exception {
        jdbcTemplate.update(
                "update quest set external_reference = ? where id = 101",
                "https://github.com/VibeSprint-Hackathon/KanBanYourself/issues/42"
        );

        String body = """
                {
                  "action": "closed",
                  "issue": {
                    "number": 42,
                    "state": "closed",
                    "repository": {
                      "full_name": "VibeSprint-Hackathon/KanBanYourself"
                    },
                    "html_url": "https://github.com/VibeSprint-Hackathon/KanBanYourself/issues/42"
                  }
                }
                """;

        mockMvc.perform(post("/api/integrations/github/issues/webhook")
                        .header("X-GitHub-Event", "issues")
                        .header("X-GitHub-Delivery", "github-delivery-42")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.quest.status").value("DONE"))
                .andExpect(jsonPath("$.player.totalXp").value(1100))
                .andExpect(jsonPath("$.source").value("GITHUB"));

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[0].status").value("DONE"));
    }

    @Test
    void ignoresDuplicateGithubDeliveryIds() throws Exception {
        jdbcTemplate.update(
                "update quest set external_reference = ? where id = 101",
                "https://github.com/VibeSprint-Hackathon/KanBanYourself/issues/42"
        );

        String body = """
                {
                  "action": "closed",
                  "issue": {
                    "number": 42,
                    "state": "closed",
                    "repository": {
                      "full_name": "VibeSprint-Hackathon/KanBanYourself"
                    },
                    "html_url": "https://github.com/VibeSprint-Hackathon/KanBanYourself/issues/42"
                  }
                }
                """;

        mockMvc.perform(post("/api/integrations/github/issues/webhook")
                        .header("X-GitHub-Event", "issues")
                        .header("X-GitHub-Delivery", "duplicate-delivery")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true));

        mockMvc.perform(post("/api/integrations/github/issues/webhook")
                        .header("X-GitHub-Event", "issues")
                        .header("X-GitHub-Delivery", "duplicate-delivery")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
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

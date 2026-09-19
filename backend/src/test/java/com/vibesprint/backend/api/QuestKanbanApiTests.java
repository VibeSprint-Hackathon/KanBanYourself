package com.vibesprint.backend.api;

import com.vibesprint.backend.integration.DemoResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestKanbanApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DemoResetService resetService;

    @BeforeEach
    void resetDemo() {
        resetService.reset();
    }

    @Test
    void createsQuestAtEndOfColumnAndPersistsForGet() throws Exception {
        String create = """
                {
                  "title":"New backlog Quest",
                  "description":"Created through the API.",
                  "status":"BACKLOG",
                  "progress":null,
                  "xpReward":250,
                  "assigneeId":1,
                  "externalReference":null
                }
                """;

        mockMvc.perform(post("/api/demo/quests").contentType("application/json").content(create))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quests", hasSize(11)))
                .andExpect(jsonPath("$.quests[2].id").value(1000))
                .andExpect(jsonPath("$.quests[2].status").value("BACKLOG"))
                .andExpect(jsonPath("$.quests[2].progress").doesNotExist())
                .andExpect(jsonPath("$.quests[2].sortOrder").value(300))
                .andExpect(jsonPath("$.quests[2].assigneeId").value(1));

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[2].id").value(1000))
                .andExpect(jsonPath("$.quests[2].title").value("New backlog Quest"));
    }

    @Test
    void exposesRosterAndCreatesQuestForChosenAssignee() throws Exception {
        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].name").value("Timofei"))
                .andExpect(jsonPath("$[1].githubLogin").value("Beresnjev"));

        String create = """
                {
                  "title":"Timofei Quest",
                  "description":"Assigned explicitly.",
                  "status":"TODO",
                  "progress":null,
                  "xpReward":100,
                  "assigneeId":2,
                  "externalReference":null
                }
                """;
        mockMvc.perform(post("/api/demo/quests").contentType("application/json").content(create))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quests[4].assigneeId").value(2));
    }

    @Test
    void rejectsUnknownAssigneeAndCompletedReassignment() throws Exception {
        String unknown = questBody("Unknown owner", "TODO", "null", 100).replace(
                "\"assigneeId\":1", "\"assigneeId\":999"
        );
        mockMvc.perform(post("/api/demo/quests").contentType("application/json").content(unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));

        String completed = questBody("Improve login error copy", "DONE", "100", 80).replace(
                "\"assigneeId\":1", "\"assigneeId\":2"
        );
        mockMvc.perform(put("/api/demo/quests/103").contentType("application/json").content(completed))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEST_CONFLICT"));
    }

    @Test
    void awardsXpToQuestAssignee() throws Exception {
        mockMvc.perform(post("/api/demo/quests/102/complete")
                        .contentType("application/json")
                        .content("{\"eventId\":\"timofei-xp\",\"source\":\"DEMO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.xpGained").value(120))
                .andExpect(jsonPath("$.player.id").value(2))
                .andExpect(jsonPath("$.player.totalXp").value(760))
                .andExpect(jsonPath("$.quest.status").value("DONE"))
                .andExpect(jsonPath("$.quest.progress").value(100));

        mockMvc.perform(get("/api/demo/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[0].totalXp").value(920))
                .andExpect(jsonPath("$.players[1].totalXp").value(760));
    }

    @Test
    void movingQuestsUpdatesCharacterStateOnlyForTheirAssignee() throws Exception {
        mockMvc.perform(patch("/api/demo/quests/101/move")
                        .contentType("application/json")
                        .content("{\"status\":\"TODO\",\"beforeQuestId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[0].characterState").value("idle"))
                .andExpect(jsonPath("$.players[1].characterState").value("idle"));

        mockMvc.perform(patch("/api/demo/quests/102/move")
                        .contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\",\"beforeQuestId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[0].characterState").value("idle"))
                .andExpect(jsonPath("$.players[1].characterState").value("coding"));

        mockMvc.perform(patch("/api/demo/quests/106/move")
                        .contentType("application/json")
                        .content("{\"status\":\"IN_PROGRESS\",\"beforeQuestId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[0].characterState").value("coding"))
                .andExpect(jsonPath("$.players[1].characterState").value("coding"));
    }

    @Test
    void rejectsCreateInDone() throws Exception {
        mockMvc.perform(post("/api/demo/quests")
                        .contentType("application/json")
                        .content(questBody("Cannot bypass completion", "DONE", "100", 250)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEST_CONFLICT"));
    }

    @Test
    void validatesProgressAgainstStatus() throws Exception {
        mockMvc.perform(post("/api/demo/quests")
                        .contentType("application/json")
                        .content(questBody("Invalid progress", "TODO", "20", 250)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void updatesEditableFieldsAndPersistsThem() throws Exception {
        String update = """
                {
                  "title":"Keyboard shortcuts revised",
                  "description":"Updated description.",
                  "status":"BACKLOG",
                  "progress":null,
                  "xpReward":260,
                  "assigneeId":2,
                  "externalReference":"https://github.com/example/repository/pull/42"
                }
                """;

        mockMvc.perform(put("/api/demo/quests/104").contentType("application/json").content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[0].id").value(104))
                .andExpect(jsonPath("$.quests[0].title").value("Keyboard shortcuts revised"))
                .andExpect(jsonPath("$.quests[0].description").value("Updated description."))
                .andExpect(jsonPath("$.quests[0].xpReward").value(260))
                .andExpect(jsonPath("$.quests[0].externalReference")
                        .value("https://github.com/example/repository/pull/42"));
    }

    @Test
    void updateMovesUnfinishedQuestToEndOfNonDoneColumn() throws Exception {
        mockMvc.perform(put("/api/demo/quests/104")
                        .contentType("application/json")
                        .content(questBody("Add keyboard shortcuts", "TESTING", "80", 250)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[7].id").value(104))
                .andExpect(jsonPath("$.quests[7].status").value("TESTING"))
                .andExpect(jsonPath("$.quests[7].progress").value(80))
                .andExpect(jsonPath("$.quests[7].sortOrder").value(400));
    }

    @Test
    void unfinishedUpdateCannotSetDone() throws Exception {
        mockMvc.perform(put("/api/demo/quests/104")
                        .contentType("application/json")
                        .content(questBody("Add keyboard shortcuts", "DONE", "100", 250)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEST_CONFLICT"));
    }

    @Test
    void completedQuestAllowsOnlyEditableFieldChanges() throws Exception {
        String update = """
                {
                  "title":"Improved login error copy",
                  "description":"Updated completed description.",
                  "status":"DONE",
                  "progress":100,
                  "xpReward":80,
                  "assigneeId":1,
                  "externalReference":"#completed"
                }
                """;

        mockMvc.perform(put("/api/demo/quests/103").contentType("application/json").content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[8].title").value("Improved login error copy"))
                .andExpect(jsonPath("$.quests[8].externalReference").value("#completed"));

        mockMvc.perform(put("/api/demo/quests/103")
                        .contentType("application/json")
                        .content(questBody("Changed reward", "DONE", "100", 81)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEST_CONFLICT"));
    }

    @Test
    void movesQuestBetweenColumnsAndNormalizesBothColumns() throws Exception {
        mockMvc.perform(patch("/api/demo/quests/104/move")
                        .contentType("application/json")
                        .content("{\"status\":\"TESTING\",\"beforeQuestId\":108}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[0].id").value(105))
                .andExpect(jsonPath("$.quests[0].sortOrder").value(100))
                .andExpect(jsonPath("$.quests[4].id").value(107))
                .andExpect(jsonPath("$.quests[5].id").value(104))
                .andExpect(jsonPath("$.quests[5].progress").value(0))
                .andExpect(jsonPath("$.quests[5].sortOrder").value(200))
                .andExpect(jsonPath("$.quests[6].id").value(108))
                .andExpect(jsonPath("$.quests[6].sortOrder").value(300));
    }

    @Test
    void reordersQuestWithinOneColumn() throws Exception {
        mockMvc.perform(patch("/api/demo/quests/109/move")
                        .contentType("application/json")
                        .content("{\"status\":\"TESTING\",\"beforeQuestId\":107}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests[5].id").value(109))
                .andExpect(jsonPath("$.quests[5].sortOrder").value(100))
                .andExpect(jsonPath("$.quests[6].id").value(107))
                .andExpect(jsonPath("$.quests[6].sortOrder").value(200))
                .andExpect(jsonPath("$.quests[7].id").value(108))
                .andExpect(jsonPath("$.quests[7].sortOrder").value(300));
    }

    @Test
    void rejectsInvalidBeforeQuestId() throws Exception {
        mockMvc.perform(patch("/api/demo/quests/104/move")
                        .contentType("application/json")
                        .content("{\"status\":\"TESTING\",\"beforeQuestId\":102}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUEST_NOT_FOUND"));
    }

    @Test
    void rejectsMoveToDoneAndMoveOfCompletedQuest() throws Exception {
        mockMvc.perform(patch("/api/demo/quests/104/move")
                        .contentType("application/json")
                        .content("{\"status\":\"DONE\",\"beforeQuestId\":null}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/demo/quests/103/move")
                        .contentType("application/json")
                        .content("{\"status\":\"TODO\",\"beforeQuestId\":null}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEST_CONFLICT"));
    }

    @Test
    void deletesOnlyUnfinishedQuestAndNormalizesColumn() throws Exception {
        mockMvc.perform(delete("/api/demo/quests/104"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests", hasSize(9)))
                .andExpect(jsonPath("$.quests[0].id").value(105))
                .andExpect(jsonPath("$.quests[0].sortOrder").value(100));

        mockMvc.perform(delete("/api/demo/quests/103"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUEST_CONFLICT"));
    }

    @Test
    void resetRemovesCreatedAndModifiedDataAndRestoresExactSeed() throws Exception {
        mockMvc.perform(post("/api/demo/quests")
                        .contentType("application/json")
                        .content(questBody("Temporary Quest", "BACKLOG", "null", 250)))
                .andExpect(status().isCreated());
        jdbcTemplate.update("update quest set title = 'Changed' where id = 101");

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quests", hasSize(10)))
                .andExpect(jsonPath("$.quests[4].id").value(101))
                .andExpect(jsonPath("$.quests[4].title").value("Fix payment validation"))
                .andExpect(jsonPath("$.quests[4].progress").value(72));

        mockMvc.perform(post("/api/demo/quests")
                        .contentType("application/json")
                        .content(questBody("After reset", "BACKLOG", "null", 250)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quests[2].id").value(1000));
    }

    private String questBody(String title, String status, String progress, int xpReward) {
        return """
                {
                  "title":"%s",
                  "description":"Test description.",
                  "status":"%s",
                  "progress":%s,
                  "xpReward":%d,
                  "assigneeId":1,
                  "externalReference":null
                }
                """.formatted(title, status, progress, xpReward);
    }
}

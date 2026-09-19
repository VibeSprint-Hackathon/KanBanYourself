package com.vibesprint.backend.raid;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RaidApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RaidRepository raidRepository;

    @Test
    void createsAndEditsDraftRaid() throws Exception {
        mockMvc.perform(post("/api/raids")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Deploy Dragon",
                                  "description":"Prepare the release candidate.",
                                  "maxHp":900,
                                  "externalReference":"milestone:12"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentHp").value(900));
        long raidId = raidRepository.findAll().stream()
                .filter(raid -> raid.getName().equals("Deploy Dragon"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/raids/{id}", raidId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Deploy Dragon Prime",
                                  "description":"Ship the release candidate.",
                                  "maxHp":1200,
                                  "externalReference":null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Deploy Dragon Prime"))
                .andExpect(jsonPath("$.maxHp").value(1200))
                .andExpect(jsonPath("$.currentHp").value(1200));
    }

    @Test
    void activatesOnlyWhenNoOtherRaidIsActive() throws Exception {
        mockMvc.perform(post("/api/raids/202/activate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RAID_CONFLICT"));

        mockMvc.perform(post("/api/raids/201/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/raids/202/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void supportsLifecycleHistoryAndDraftDeletionRules() throws Exception {
        mockMvc.perform(post("/api/raids/201/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentHp").value(0));
        mockMvc.perform(delete("/api/raids/201"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RAID_CONFLICT"));

        mockMvc.perform(delete("/api/raids/202"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/raids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void cannotDeleteActiveRaidAndReturnsStableNotFoundError() throws Exception {
        mockMvc.perform(delete("/api/raids/201"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RAID_CONFLICT"));
        mockMvc.perform(get("/api/raids/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RAID_NOT_FOUND"));
    }

    @Test
    void preservesDamageWhenActiveMaxHpChanges() throws Exception {
        mockMvc.perform(put("/api/raids/201")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name":"Merge Conflict Hydra",
                                  "description":"Updated metadata.",
                                  "maxHp":800,
                                  "externalReference":null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxHp").value(800))
                .andExpect(jsonPath("$.currentHp").value(0))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}

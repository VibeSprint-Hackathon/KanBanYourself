package com.vibesprint.backend.integration;

import com.vibesprint.backend.api.DemoStateQueryService;
import com.vibesprint.backend.api.DemoStateResponse;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoResetService {

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final DemoStateQueryService stateQueryService;

    public DemoResetService(
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            DemoStateQueryService stateQueryService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.stateQueryService = stateQueryService;
    }

    @Transactional
    public DemoStateResponse reset() {
        jdbcTemplate.execute("lock table quest, player, raid in access exclusive mode");
        jdbcTemplate.update("delete from quest");
        jdbcTemplate.update("delete from raid");
        jdbcTemplate.update("delete from player");

        jdbcTemplate.update(
                "insert into player (id, name, total_xp) values (?, ?, ?)",
                1L,
                "Andrei",
                920
        );
        insertQuest(101L, "Fix payment validation", "IN_PROGRESS", 180);
        insertQuest(102L, "Add rate limit warning", "TODO", 120);
        insertQuest(103L, "Improve login error copy", "DONE", 80);
        jdbcTemplate.update(
                "insert into raid (id, name, max_hp, current_hp) values (?, ?, ?, ?)",
                201L,
                "Merge Conflict Hydra",
                1000,
                180
        );

        entityManager.clear();
        return stateQueryService.getState();
    }

    private void insertQuest(long id, String title, String status, int xpReward) {
        jdbcTemplate.update(
                """
                insert into quest (id, title, status, xp_reward, assignee_id, external_reference)
                values (?, ?, ?, ?, ?, null)
                """,
                id,
                title,
                status,
                xpReward,
                1L
        );
    }
}

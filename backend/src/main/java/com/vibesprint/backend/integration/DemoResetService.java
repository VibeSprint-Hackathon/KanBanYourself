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
        jdbcTemplate.execute("alter sequence quest_id_seq restart with 1000");

        jdbcTemplate.update(
                "insert into player (id, name, total_xp) values (?, ?, ?)",
                1L,
                "Andrei",
                920
        );
        insertQuest(
                101L,
                "Fix payment validation",
                "Fix server-side validation and cover the payment edge cases.",
                "IN_PROGRESS",
                72,
                180,
                null,
                100
        );
        insertQuest(
                102L,
                "Add rate limit warning",
                "Warn users before API traffic reaches the configured rate limit.",
                "TODO",
                null,
                120,
                null,
                100
        );
        insertQuest(
                103L,
                "Improve login error copy",
                "Make login failures clearer and easier to resolve.",
                "DONE",
                100,
                80,
                null,
                100
        );
        insertQuest(104L, "Add keyboard shortcuts", "Speed up the command palette and common actions.",
                "BACKLOG", null, 250, null, 100);
        insertQuest(105L, "Improve loading states", "Make every wait feel clear and intentional.",
                "BACKLOG", null, 180, null, 200);
        insertQuest(106L, "Add achievement filters", "Find earned badges by category and rarity.",
                "TODO", null, 320, null, 200);
        insertQuest(107L, "Add profile rewards", "Surface earned badges on the player profile.",
                "TESTING", 35, 420, null, 100);
        insertQuest(108L, "Validate GitHub webhook", "Check event signatures and safe handling of retries.",
                "TESTING", 90, 350, "#148 Webhook validation", 200);
        insertQuest(109L, "Test level-up animation", "Review the reward moment and reduced motion state.",
                "TESTING", 85, 280, null, 300);
        insertQuest(110L, "Connect GitHub", "Link commits and pull requests to Quest progress.",
                "DONE", 100, 300, null, 200);
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

    private void insertQuest(
            long id,
            String title,
            String description,
            String status,
            Integer progress,
            int xpReward,
            String externalReference,
            int sortOrder
    ) {
        jdbcTemplate.update(
                """
                insert into quest
                    (id, title, description, status, progress, xp_reward, assignee_id, external_reference, sort_order)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                title,
                description,
                status,
                progress,
                xpReward,
                1L,
                externalReference,
                sortOrder
        );
    }
}

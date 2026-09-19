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
        jdbcTemplate.execute("lock table quest, player, raid, player_achievement in access exclusive mode");
        jdbcTemplate.update("delete from player_achievement");
        jdbcTemplate.update("delete from quest");
        jdbcTemplate.update("delete from raid");
        jdbcTemplate.update("delete from player");
        jdbcTemplate.execute("alter sequence quest_id_seq restart with 1000");
        jdbcTemplate.execute("alter sequence raid_id_seq restart with 1000");

        jdbcTemplate.update(
                "insert into player (id, name, total_xp, github_login, github_user_id) values (?, ?, ?, ?, ?)",
                1L,
                "Andrei",
                920,
                "amjastsov",
                117397316L
        );
        insertPlayer(2L, "Timofei", 640, "Beresnjev", 22981929L);
        insertPlayer(3L, "Nikita", 360, "Parsifal22", 73550345L);
        insertQuest(
                101L,
                "Fix payment validation",
                "Fix server-side validation and cover the payment edge cases.",
                "IN_PROGRESS",
                72,
                180,
                1L,
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
                2L,
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
                1L,
                null,
                100
        );
        insertQuest(104L, "Add keyboard shortcuts", "Speed up the command palette and common actions.",
                "BACKLOG", null, 250, 2L, null, 100);
        insertQuest(105L, "Improve loading states", "Make every wait feel clear and intentional.",
                "BACKLOG", null, 180, 3L, null, 200);
        insertQuest(106L, "Add achievement filters", "Find earned badges by category and rarity.",
                "TODO", null, 320, 1L, null, 200);
        insertQuest(107L, "Add profile rewards", "Surface earned badges on the player profile.",
                "TESTING", 35, 420, 3L, null, 100);
        insertQuest(108L, "Validate GitHub webhook", "Check event signatures and safe handling of retries.",
                "TESTING", 90, 350, 2L, "#148 Webhook validation", 200);
        insertQuest(109L, "Test level-up animation", "Review the reward moment and reduced motion state.",
                "TESTING", 85, 280, 1L, null, 300);
        insertQuest(110L, "Connect GitHub", "Link commits and pull requests to Quest progress.",
                "DONE", 100, 300, 3L, null, 200);
        insertRaid(
                201L,
                "Merge Conflict Hydra",
                "Defeat the merge conflicts blocking the team sprint.",
                1000,
                180,
                "ACTIVE",
                null
        );
        insertRaid(
                202L,
                "Release Deadline Golem",
                "Prepare the next release and clear the final blockers.",
                1600,
                1600,
                "DRAFT",
                null
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
            long assigneeId,
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
                assigneeId,
                externalReference,
                sortOrder
        );
    }

    private void insertPlayer(long id, String name, int totalXp, String githubLogin, long githubUserId) {
        jdbcTemplate.update(
                "insert into player (id, name, total_xp, github_login, github_user_id) values (?, ?, ?, ?, ?)",
                id, name, totalXp, githubLogin, githubUserId
        );
    }

    private void insertRaid(
            long id,
            String name,
            String description,
            int maxHp,
            int currentHp,
            String status,
            String externalReference
    ) {
        jdbcTemplate.update(
                """
                insert into raid
                    (id, name, description, max_hp, current_hp, status, external_reference)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                name,
                description,
                maxHp,
                currentHp,
                status,
                externalReference
        );
    }
}

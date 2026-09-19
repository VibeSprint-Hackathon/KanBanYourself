package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.progression.InvalidProgressionCommandException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class GitHubQuestSyncService {
    private final QuestRepository quests;
    private final PlayerRepository players;
    private final JdbcTemplate jdbc;
    private final String repository;

    public GitHubQuestSyncService(QuestRepository quests, PlayerRepository players, JdbcTemplate jdbc,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String repository) {
        this.quests = quests;
        this.players = players;
        this.jdbc = jdbc;
        this.repository = repository;
    }

    @Transactional
    public int importIssues(List<GitHubIssueResponse> issues) {
        for (GitHubIssueResponse issue : issues) upsert(issue, false);
        return issues.size();
    }

    @Transactional
    public Quest upsert(GitHubIssueResponse issue, boolean liveClosure) {
        if (issue.number() <= 0 || !repository.equalsIgnoreCase(issue.repository())) {
            throw new InvalidProgressionCommandException("Invalid GitHub issue repository or number");
        }
        // Serialize imports and webhook upserts, including the first insert of a new issue.
        jdbc.execute("select pg_advisory_xact_lock(7821934)");
        String url = "https://github.com/" + repository + "/issues/" + issue.number();
        Quest quest = quests.findByExternalReference(url)
                .map(existing -> quests.findByIdForUpdate(existing.getId()).orElseThrow()).orElse(null);
        String title = issue.title();
        if (title == null || title.isBlank()) {
            if (quest != null) return quest; // Older minimal closure payloads contain only identity.
            throw new InvalidProgressionCommandException("GitHub issue title is required");
        }
        if (title.length() > 255) title = title.substring(0, 255);
        String body = issue.body() == null ? "" : issue.body();
        Player assignee = issue.assigneeLogin() == null ? null
                : players.findByGithubLoginIgnoreCase(issue.assigneeLogin()).orElse(null);
        if (assignee == null) assignee = quest != null ? quest.getAssignee() : players.findById(1L).orElseThrow();
        QuestStatus status = liveClosure ? QuestStatus.TODO : status(issue);
        Integer progress = switch (status) {
            case DONE -> 100;
            case IN_PROGRESS -> 50;
            default -> null;
        };
        if (quest == null) {
            return quests.save(Quest.create(title, body, status, progress, 100, assignee, url, nextOrder(status)));
        }
        quest.updateEditableFields(title, body, url);
        // Completed quests remain completed: reopening must not allow a second XP award.
        if (quest.getStatus() == QuestStatus.DONE) return quest;
        quest.assignTo(assignee);
        if (liveClosure) return quest;
        if (status == QuestStatus.DONE) {
            quest.complete(nextOrder(status)); // Historical/label sync never awards retroactive XP.
        } else {
            int order = status == quest.getStatus() ? quest.getSortOrder() : nextOrder(status);
            quest.updateUnfinished(title, body, status, progress, quest.getXpReward(), assignee, url, order);
        }
        return quest;
    }

    private int nextOrder(QuestStatus status) {
        return quests.findAllByStatusOrderBySortOrderAscIdAsc(status).stream()
                .mapToInt(Quest::getSortOrder).max().orElse(0) + 100;
    }

    static QuestStatus status(GitHubIssueResponse issue) {
        List<String> labels = issue.labels() == null ? List.of() : issue.labels().stream()
                .filter(java.util.Objects::nonNull)
                .map(label -> label.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-')).toList();
        if ("closed".equalsIgnoreCase(issue.state()) || labels.contains("done") || labels.contains("completed")) return QuestStatus.DONE;
        if (labels.contains("testing") || labels.contains("in-review")) return QuestStatus.TESTING;
        if (labels.contains("in-progress") || labels.contains("inprogress") || labels.contains("active")) return QuestStatus.IN_PROGRESS;
        if (labels.contains("backlog")) return QuestStatus.BACKLOG;
        return QuestStatus.TODO;
    }
}

package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.api.DemoStateResponse;
import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.quest.QuestStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GitHubRuntimeIssueStore {

    private final Map<String, DemoStateResponse.QuestView> runtimeIssues = new ConcurrentHashMap<>();

    public void upsert(GitHubIssueResponse issue, QuestStatus status, Player assignee, Long persistedQuestId) {
        if (issue == null) {
            return;
        }

        String externalReference = firstNonBlank(issue.htmlUrl(), issue.url());
        if (externalReference == null || externalReference.isBlank()) {
            return;
        }

        int progress = switch (status) {
            case TODO -> 0;
            case IN_PROGRESS -> 50;
            case BACKLOG -> 0;
            case TESTING -> 50;
            case DONE -> 100;
        };

        long questId = persistedQuestId != null ? persistedQuestId : Math.abs(externalReference.hashCode());

        DemoStateResponse.QuestView runtimeQuest = new DemoStateResponse.QuestView(
                questId,
                issue.title() == null ? "GitHub issue" : issue.title(),
                issue.body() == null ? "" : issue.body(),
                status.name(),
                progress,
                0,
                assignee != null ? assignee.getId() : 1L,
                externalReference,
                sortOrder(status)
        );

        runtimeIssues.put(externalReference, runtimeQuest);
    }

    public List<DemoStateResponse.QuestView> snapshot() {
        return new ArrayList<>(runtimeIssues.values());
    }

    public void clear() {
        runtimeIssues.clear();
    }

    private int sortOrder(QuestStatus status) {
        return switch (status) {
            case BACKLOG -> 100;
            case TODO -> 200;
            case IN_PROGRESS -> 300;
            case TESTING -> 400;
            case DONE -> 500;
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

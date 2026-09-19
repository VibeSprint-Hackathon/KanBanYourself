package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class GitHubIssueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubIssueService.class);
    private static final long DEMO_PLAYER_ID = 1L;

    private final RestTemplate githubRestTemplate;
    private final QuestRepository questRepository;
    private final PlayerRepository playerRepository;
    private final String repository;
    private final String apiBaseUrl;
    private final String token;

    public GitHubIssueService(
            RestTemplate githubRestTemplate,
            QuestRepository questRepository,
            PlayerRepository playerRepository,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String repository,
            @Value("${app.github.api-base-url:https://api.github.com}") String apiBaseUrl,
            @Value("${app.github.token:}") String token
    ) {
        this.githubRestTemplate = githubRestTemplate;
        this.questRepository = questRepository;
        this.playerRepository = playerRepository;
        this.repository = repository;
        this.token = token;
        this.apiBaseUrl = apiBaseUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logLiveIssuesOnStartup() {
        try {
            List<GitHubIssueResponse> issues = fetchIssues();
            syncIssuesToBoard(issues);
            LOGGER.info("GitHub integration active for repository {}. Loaded {} issue(s).", repository, issues.size());
            issues.stream().limit(10).forEach(issue ->
                    LOGGER.info("GitHub issue #{}: {} [{}] {} [{}]", issue.number(), issue.title(), issue.state(), issue.htmlUrl(), mapStatus(issue.labels())));
        } catch (IllegalStateException ex) {
            LOGGER.warn("GitHub live issue sync is unavailable for repository {}: {}", repository, ex.getMessage());
        }
    }

    @Transactional
    public void syncIssuesToBoard(List<GitHubIssueResponse> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }

        for (GitHubIssueResponse issue : issues) {
            if (issue == null) {
                continue;
            }

            String externalReference = firstNonBlank(issue.htmlUrl(), issue.url());
            if (externalReference == null || externalReference.isBlank()) {
                continue;
            }

            QuestStatus status = mapStatus(issue.labels());
            Quest quest = questRepository.findByExternalReference(externalReference)
                    .orElseGet(() -> createQuestFromIssue(issue, status));

            if (status == QuestStatus.DONE) {
                if (quest.getStatus() != QuestStatus.DONE) {
                    quest.complete(nextSortOrder(status));
                }
                quest.updateEditableFields(issue.title(), issue.body() == null ? "" : issue.body(), externalReference);
                questRepository.save(quest);
                continue;
            }

            if (quest.getStatus() == QuestStatus.DONE) {
                continue;
            }

            Integer progress = switch (status) {
                case TODO -> null;
                case IN_PROGRESS -> quest.getProgress() != null ? quest.getProgress() : 50;
                case BACKLOG, TESTING -> null;
                case DONE -> 100;
            };

            quest.updateUnfinished(
                    issue.title(),
                    issue.body() == null ? "" : issue.body(),
                    status,
                    progress,
                    quest.getXpReward(),
                    externalReference,
                    nextSortOrder(status)
            );
            questRepository.save(quest);
        }
    }

    public List<GitHubIssueResponse> fetchIssues() {
        String issueUrl = apiBaseUrl + "/repos/" + repository + "/issues?state=open&per_page=100";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("User-Agent", "VibeSprint-Backend");
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = githubRestTemplate.exchange(issueUrl, HttpMethod.GET, request, List.class);
            List<Map<String, Object>> issuePayload = response.getBody() == null ? List.of() : response.getBody();

            List<GitHubIssueResponse> issues = new ArrayList<>();
            for (Map<String, Object> issueMap : issuePayload) {
                if (issueMap.containsKey("pull_request")) {
                    continue;
                }
                issues.add(new GitHubIssueResponse(
                        asLong(issueMap.get("id")),
                        asInt(issueMap.get("number")),
                        asString(issueMap.get("title")),
                        asString(issueMap.get("state")),
                        asString(issueMap.get("url")),
                        asString(issueMap.get("html_url")),
                        repository,
                        asString(issueMap.get("body")),
                        asStringList(issueMap.get("labels"))
                ));
            }
            return issues;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalStateException(
                        "GitHub repository not found or not accessible publicly: " + repository + ". Set GITHUB_REPOSITORY and GITHUB_TOKEN if needed."
                );
            }
            throw new IllegalStateException("GitHub API request failed: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException(
                    "GitHub is unreachable from this environment: " + e.getMessage() + ". Check DNS/network access and proxy settings.",
                    e
            );
        }
    }

    public static QuestStatus mapStatus(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return QuestStatus.TODO;
        }

        for (String label : labels) {
            String normalized = normalizeLabel(label);
            if (normalized.isBlank()) {
                continue;
            }
            if (normalized.contains("done") || normalized.contains("completed")) {
                return QuestStatus.DONE;
            }
            if (normalized.contains("in-progress") || normalized.contains("inprogress") || normalized.contains("active")) {
                return QuestStatus.IN_PROGRESS;
            }
            if (normalized.contains("todo") || normalized.contains("to-do") || normalized.contains("backlog")) {
                return QuestStatus.TODO;
            }
        }
        return QuestStatus.TODO;
    }

    private Quest createQuestFromIssue(GitHubIssueResponse issue, QuestStatus status) {
        Player assignee = playerRepository.findById(DEMO_PLAYER_ID)
                .orElseThrow(() -> new IllegalStateException("Player " + DEMO_PLAYER_ID + " is not ready"));

        Integer progress = switch (status) {
            case TODO -> null;
            case IN_PROGRESS -> 50;
            case BACKLOG, TESTING -> null;
            case DONE -> 100;
        };

        Quest quest = Quest.create(
                issue.title(),
                issue.body() == null ? "" : issue.body(),
                status,
                progress,
                Math.max(10, issue.number() * 25),
                assignee,
                firstNonBlank(issue.htmlUrl(), issue.url()),
                nextSortOrder(status)
        );
        return questRepository.save(quest);
    }

    private int nextSortOrder(QuestStatus status) {
        return questRepository.findAllByStatusOrderBySortOrderAscIdAsc(status).stream()
                .mapToInt(Quest::getSortOrder)
                .max()
                .orElse(0) + 100;
    }

    private static String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(label -> !label.isBlank())
                    .toList();
        }
        return List.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}

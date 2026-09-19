package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.api.DemoStateResponse;
import com.vibesprint.backend.api.ProgressionResponse;
import com.vibesprint.backend.player.Player;
import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import com.vibesprint.backend.realtime.ProgressionRealtimePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
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
    private final ProgressionRealtimePublisher realtimePublisher;
    private final String githubSyncEnabledSetting;
    private final String repository;
    private final String apiBaseUrl;
    private final String token;
    private final GitHubRuntimeIssueStore runtimeIssueStore;
    private final boolean runtimeCacheOnly;

    public GitHubIssueService(
            RestTemplate githubRestTemplate,
            QuestRepository questRepository,
            PlayerRepository playerRepository,
            String repository,
            String apiBaseUrl,
            String token
    ) {
        this(githubRestTemplate, questRepository, playerRepository, null, "", repository, apiBaseUrl, token, null, false);
    }

    @Autowired
    public GitHubIssueService(
            RestTemplate githubRestTemplate,
            QuestRepository questRepository,
            PlayerRepository playerRepository,
            ProgressionRealtimePublisher realtimePublisher,
            @Value("${app.github.enabled:}") String githubSyncEnabledSetting,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String repository,
            @Value("${app.github.api-base-url:https://api.github.com}") String apiBaseUrl,
            @Value("${app.github.token:}") String token,
            GitHubRuntimeIssueStore runtimeIssueStore,
            @Value("${app.github.runtime-cache-only:true}") boolean runtimeCacheOnly
    ) {
        this.githubRestTemplate = githubRestTemplate;
        this.questRepository = questRepository;
        this.playerRepository = playerRepository;
        this.realtimePublisher = realtimePublisher;
        this.githubSyncEnabledSetting = githubSyncEnabledSetting;
        this.repository = repository;
        this.token = token;
        this.apiBaseUrl = apiBaseUrl;
        this.runtimeIssueStore = runtimeIssueStore;
        this.runtimeCacheOnly = runtimeCacheOnly;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logLiveIssuesOnStartup() {
        if (!githubSyncEnabled()) {
            LOGGER.debug("GitHub sync is disabled because no token was configured or GITHUB_SYNC_ENABLED is false.");
            return;
        }
        refreshIssuesFromGitHub();
    }

    @Scheduled(fixedDelayString = "${app.github.sync-delay-ms:30000}")
    public void refreshIssuesFromGitHub() {
        if (!githubSyncEnabled()) {
            return;
        }

        try {
            if (runtimeCacheOnly) {
                runtimeIssueStore.clear();
            }
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

            Player assignee = resolveAssignee(issue.assigneeLogin());
            QuestStatus status = mapStatus(issue.labels());

            Quest quest = questRepository.findByExternalReference(externalReference)
                    .orElse(null);

            if (runtimeCacheOnly) {
                runtimeIssueStore.upsert(issue, status, assignee, quest != null ? quest.getId() : null);
                continue;
            }
            if (quest == null) {
                try {
                    quest = createQuestFromIssue(issue, status, assignee);
                } catch (Exception duplicateInsert) {
                    quest = questRepository.findByExternalReference(externalReference)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Duplicate GitHub issue in sync for " + externalReference,
                                    duplicateInsert
                            ));
                }
            }

            if (quest.getAssignee() == null || !quest.getAssignee().getId().equals(assignee.getId())) {
                quest.assignTo(assignee);
            }

            if (status == QuestStatus.DONE) {
                if (quest.getStatus() != QuestStatus.DONE) {
                    quest.complete(nextSortOrder(status));
                }
                quest.updateEditableFields(issue.title(), issue.body() == null ? "" : issue.body(), externalReference);
                questRepository.save(quest);
                publishGitHubSync(quest, assignee, issue.number(), issue.title());
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
                    quest.getAssignee(),
                    externalReference,
                    nextSortOrder(status)
            );
            questRepository.save(quest);
            publishGitHubSync(quest, assignee, issue.number(), issue.title());
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
                Map<String, Object> assigneeMap = issueMap.containsKey("assignee") && issueMap.get("assignee") instanceof Map<?, ?>
                        ? (Map<String, Object>) issueMap.get("assignee")
                        : Map.of();

                issues.add(new GitHubIssueResponse(
                        asLong(issueMap.get("id")),
                        asInt(issueMap.get("number")),
                        asString(issueMap.get("title")),
                        asString(issueMap.get("state")),
                        asString(issueMap.get("url")),
                        asString(issueMap.get("html_url")),
                        repository,
                        asString(issueMap.get("body")),
                        asString(assigneeMap.get("login")),
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

    private boolean githubSyncEnabled() {
        String configuredFlag = firstNonBlank(
                githubSyncEnabledSetting,
                System.getenv("GITHUB_SYNC_ENABLED"),
                System.getProperty("app.github.enabled")
        );
        if (configuredFlag != null) {
            return Boolean.parseBoolean(configuredFlag);
        }
        return false;
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

    private Quest createQuestFromIssue(GitHubIssueResponse issue, QuestStatus status, Player assignee) {
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

    private void publishGitHubSync(Quest quest, Player assignee, int issueNumber, String title) {
        if (realtimePublisher == null || assignee == null) {
            return;
        }

        int currentLevel = 1;
        Integer nextLevelXp = null;
        ProgressionResponse response = new ProgressionResponse(
                "github-sync-" + issueNumber,
                true,
                null,
                0,
                0,
                false,
                null,
                null,
                false,
                new DemoStateResponse.QuestView(
                        quest.getId(),
                        title,
                        quest.getDescription(),
                        quest.getStatus().name(),
                        quest.getProgress(),
                        quest.getXpReward(),
                        assignee.getId(),
                        quest.getExternalReference(),
                        quest.getSortOrder()
                ),
                new DemoStateResponse.PlayerView(
                        assignee.getId(),
                        assignee.getName(),
                        assignee.getGithubLogin(),
                        assignee.getTotalXp(),
                        currentLevel,
                        nextLevelXp,
                        "Developer",
                        null,
                        "coding",
                        null
                ),
                null,
                "GITHUB",
                false
        );
        realtimePublisher.publish(response);
    }

    private Player resolveAssignee(String assigneeLogin) {
        Player fallbackPlayer = playerRepository.findById(DEMO_PLAYER_ID)
                .orElseThrow(() -> new IllegalStateException("Player " + DEMO_PLAYER_ID + " is not ready"));

        if (assigneeLogin == null || assigneeLogin.isBlank()) {
            return fallbackPlayer;
        }

        for (Player candidate : playerRepository.findAll()) {
            if (candidate.getName() != null && candidate.getName().equalsIgnoreCase(assigneeLogin)) {
                return candidate;
            }
        }

        return playerRepository.save(createPlayerFromGitHubLogin(assigneeLogin, fallbackPlayer.getId()));
    }

    private Player createPlayerFromGitHubLogin(String assigneeLogin, long fallbackId) {
        long nextId = playerRepository.findAll().stream()
                .mapToLong(Player::getId)
                .max()
                .orElse(fallbackId) + 1;

        try {
            Player player = Player.class.getDeclaredConstructor().newInstance();
            setField(player, "id", nextId);
            setField(player, "name", assigneeLogin);
            setField(player, "totalXp", 0);
            return player;
        } catch (Exception exception) {
            LOGGER.warn("Could not create a GitHub assignee player for {}. Falling back to demo player {}.", assigneeLogin, fallbackId);
            return fallbackPlayerOrThrow(fallbackId);
        }
    }

    private Player fallbackPlayerOrThrow(long fallbackId) {
        return playerRepository.findById(fallbackId)
                .orElseThrow(() -> new IllegalStateException("Player " + fallbackId + " is not ready"));
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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

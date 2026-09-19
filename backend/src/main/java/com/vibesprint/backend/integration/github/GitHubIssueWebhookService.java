package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.api.DemoApiMapper;
import com.vibesprint.backend.api.ProgressionResponse;
import com.vibesprint.backend.progression.CompleteQuestCommand;
import com.vibesprint.backend.progression.InvalidProgressionCommandException;
import com.vibesprint.backend.progression.ProgressionResult;
import com.vibesprint.backend.progression.ProgressionService;
import com.vibesprint.backend.progression.ProgressionSource;
import com.vibesprint.backend.quest.Quest;
import com.vibesprint.backend.quest.QuestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubIssueWebhookService {

    private final QuestRepository questRepository;
    private final ProgressionService progressionService;
    private final DemoApiMapper mapper;
    private final GitHubIssueService githubIssueService;
    private final Set<String> processedDeliveries = ConcurrentHashMap.newKeySet();
    private final String acceptedRepository;

    public GitHubIssueWebhookService(
            QuestRepository questRepository,
            ProgressionService progressionService,
            DemoApiMapper mapper,
            GitHubIssueService githubIssueService,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String acceptedRepository
    ) {
        this.questRepository = questRepository;
        this.progressionService = progressionService;
        this.mapper = mapper;
        this.githubIssueService = githubIssueService;
        this.acceptedRepository = acceptedRepository;
    }

    public ProgressionResponse process(String eventType, String deliveryId, Map<String, Object> payload) {
        if (deliveryId == null || deliveryId.isBlank()) {
            throw new InvalidProgressionCommandException("GitHub delivery id is required");
        }
        if (!"issues".equalsIgnoreCase(eventType)) {
            throw new InvalidProgressionCommandException("Only issues events are supported");
        }
        if (!processedDeliveries.add(deliveryId)) {
            return duplicateResponse(deliveryId);
        }

        Map<String, Object> issueNode = asMap(payload == null ? null : payload.get("issue"));
        if (issueNode == null) {
            throw new InvalidProgressionCommandException("GitHub issue payload is required");
        }

        String action = asString(payload, "action");
        Set<String> supportedActions = Set.of("opened", "reopened", "edited", "labeled", "unlabeled", "assigned", "unassigned", "closed");
        if (!supportedActions.contains(action == null ? "" : action.toLowerCase())) {
            throw new InvalidProgressionCommandException("Unsupported GitHub issue action: " + action);
        }

        String issueUrl = normalizeIssueUrl(issueNode, payload);
        if (issueUrl == null || issueUrl.isBlank()) {
            throw new InvalidProgressionCommandException("GitHub issue URL is required");
        }

        String repositoryFromUrl = repositoryFromIssueUrl(issueUrl);
        if (repositoryFromUrl == null || !repositoryFromUrl.equalsIgnoreCase(acceptedRepository)) {
            throw new InvalidProgressionCommandException("Only issues from repository " + acceptedRepository + " are accepted");
        }

        if ("closed".equalsIgnoreCase(action)) {
            Quest linkedQuest = questRepository.findByExternalReference(issueUrl)
                    .orElseThrow(() -> new InvalidProgressionCommandException("No quest is linked to GitHub issue " + issueUrl));

            ProgressionResult result = progressionService.completeQuest(
                    new CompleteQuestCommand(linkedQuest.getId(), deliveryId, ProgressionSource.GITHUB)
            );
            return mapper.toResponse(result, "GITHUB", false);
        }

        githubIssueService.syncIssuesToBoard(List.of(toIssueResponse(issueNode, payload)));
        return new ProgressionResponse(
                deliveryId,
                true,
                "GITHUB_SYNC",
                0,
                0,
                false,
                null,
                "updated",
                false,
                null,
                null,
                null,
                "GITHUB",
                false
        );
    }

    private String normalizeIssueUrl(Map<String, Object> issueNode, Map<String, Object> payload) {
        String fromIssueUrl = asString(issueNode, "html_url");
        if (fromIssueUrl != null && !fromIssueUrl.isBlank()) {
            return fromIssueUrl;
        }

        Map<String, Object> repositoryNode = asMap(issueNode.get("repository"));
        String repository = repositoryNode == null ? null : asString(repositoryNode, "full_name");
        String issueNumber = asString(issueNode, "number");
        if (repository != null && !repository.isBlank() && issueNumber != null && !issueNumber.isBlank()) {
            return "https://github.com/" + repository + "/issues/" + issueNumber;
        }

        Map<String, Object> rootRepositoryNode = asMap(payload == null ? null : payload.get("repository"));
        if (rootRepositoryNode != null) {
            String repoName = asString(rootRepositoryNode, "full_name");
            if (repoName != null && !repoName.isBlank() && issueNumber != null && !issueNumber.isBlank()) {
                return "https://github.com/" + repoName + "/issues/" + issueNumber;
            }
        }
        return null;
    }

    private String repositoryFromIssueUrl(String issueUrl) {
        if (issueUrl == null || issueUrl.isBlank()) {
            return null;
        }
        String normalized = issueUrl.trim();
        if (!normalized.startsWith("https://github.com/")) {
            return null;
        }
        String path = normalized.substring("https://github.com/".length());
        int issueIndex = path.indexOf("/issues/");
        if (issueIndex <= 0) {
            return null;
        }
        return path.substring(0, issueIndex);
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String asString(Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }

    private GitHubIssueResponse toIssueResponse(Map<String, Object> issueNode, Map<String, Object> payload) {
        Map<String, Object> repositoryNode = asMap(issueNode.get("repository"));
        String repositoryName = repositoryNode == null ? null : asString(repositoryNode, "full_name");
        if (repositoryName == null || repositoryName.isBlank()) {
            Map<String, Object> rootRepositoryNode = asMap(payload == null ? null : payload.get("repository"));
            repositoryName = rootRepositoryNode == null ? acceptedRepository : asString(rootRepositoryNode, "full_name");
        }

        Map<String, Object> assigneeNode = asMap(issueNode.get("assignee"));
        String assigneeLogin = assigneeNode == null ? null : asString(assigneeNode, "login");

        return new GitHubIssueResponse(
                asLong(issueNode.get("id")),
                asInt(issueNode.get("number")),
                asString(issueNode, "title"),
                asString(issueNode, "state"),
                asString(issueNode, "url"),
                asString(issueNode, "html_url"),
                repositoryName == null ? acceptedRepository : repositoryName,
                asString(issueNode, "body"),
                assigneeLogin,
                extractLabels(issueNode.get("labels"))
        );
    }

    private List<String> extractLabels(Object labelsValue) {
        if (!(labelsValue instanceof java.util.List<?> labelsList)) {
            return java.util.List.of();
        }

        java.util.List<String> labels = new java.util.ArrayList<>();
        for (Object item : labelsList) {
            if (item instanceof Map<?, ?> map) {
                Object name = map.get("name");
                if (name != null) {
                    labels.add(name.toString());
                }
            } else if (item != null) {
                labels.add(item.toString());
            }
        }
        return labels;
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

    private int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private ProgressionResponse duplicateResponse(String deliveryId) {
        return new ProgressionResponse(
                deliveryId,
                false,
                "DUPLICATE_EVENT",
                0,
                0,
                false,
                null,
                null,
                false,
                null,
                null,
                null,
                "GITHUB",
                true
        );
    }
}

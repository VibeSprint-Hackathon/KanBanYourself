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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GitHubIssueWebhookService {

    private final QuestRepository questRepository;
    private final ProgressionService progressionService;
    private final DemoApiMapper mapper;
    private final GitHubWebhookDeliveryStore deliveryStore;
    private final String acceptedRepository;
    private final GitHubQuestSyncService syncService;

    public GitHubIssueWebhookService(
            QuestRepository questRepository,
            ProgressionService progressionService,
            DemoApiMapper mapper,
            GitHubWebhookDeliveryStore deliveryStore,
            GitHubQuestSyncService syncService,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String acceptedRepository
    ) {
        this.questRepository = questRepository;
        this.progressionService = progressionService;
        this.mapper = mapper;
        this.deliveryStore = deliveryStore;
        this.syncService = syncService;
        this.acceptedRepository = acceptedRepository;
    }

    @Transactional
    public ProgressionResponse process(String eventType, String deliveryId, Map<String, Object> payload) {
        validateHeaders(eventType, deliveryId);

        String action = asString(payload, "action");
        if (action == null || !java.util.Set.of("opened", "reopened", "edited", "labeled", "unlabeled", "assigned", "unassigned", "closed").contains(action)) {
            throw new InvalidProgressionCommandException("Unsupported GitHub issue action");
        }

        Map<String, Object> issueNode = asMap(payload == null ? null : payload.get("issue"));
        if (issueNode == null) {
            throw new InvalidProgressionCommandException("GitHub issue payload is required");
        }

        String issueUrl = canonicalIssueUrl(issueNode, payload);
        if (!deliveryStore.tryRegister(deliveryId)) {
            return duplicateResponse(deliveryId);
        }

        Map<String, Object> assignee = asMap(issueNode.get("assignee"));
        java.util.List<String> labels = issueNode.get("labels") instanceof java.util.List<?> values
                ? values.stream().map(this::asMap).filter(java.util.Objects::nonNull)
                    .map(label -> asString(label, "name")).filter(java.util.Objects::nonNull).toList()
                : java.util.List.of();
        Quest linkedQuest = syncService.upsert(new GitHubIssueResponse(
                0, (int) asPositiveLong(issueNode.get("number")), asString(issueNode, "title"),
                asString(issueNode, "state"), issueUrl, issueUrl, acceptedRepository,
                asString(issueNode, "body"), asString(assignee, "login"), labels), "closed".equals(action));
        if (!"closed".equals(action)) {
            return new ProgressionResponse(deliveryId, false, "GITHUB_SYNC", 0, 0, false,
                    null, null, false, java.util.List.of(), mapper.toQuest(linkedQuest), null, null, "GITHUB", false);
        }

        ProgressionResult result = progressionService.completeQuest(
                new CompleteQuestCommand(linkedQuest.getId(), deliveryId, ProgressionSource.GITHUB)
        );
        return mapper.toResponse(result, "GITHUB", false);
    }

    private void validateHeaders(String eventType, String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank() || deliveryId.length() > 100) {
            throw new InvalidProgressionCommandException("Valid GitHub delivery id is required");
        }
        if (!"issues".equalsIgnoreCase(eventType)) {
            throw new InvalidProgressionCommandException("Only issues events are supported");
        }
    }

    private String canonicalIssueUrl(Map<String, Object> issueNode, Map<String, Object> payload) {
        Map<String, Object> repositoryNode = asMap(payload == null ? null : payload.get("repository"));
        String repository = repositoryNode == null ? null : asString(repositoryNode, "full_name");
        if (repository == null || !repository.equalsIgnoreCase(acceptedRepository)) {
            throw new InvalidProgressionCommandException(
                    "Only issues from repository " + acceptedRepository + " are accepted"
            );
        }

        long issueNumber = asPositiveLong(issueNode.get("number"));
        return "https://github.com/" + acceptedRepository + "/issues/" + issueNumber;
    }

    private long asPositiveLong(Object value) {
        try {
            long number = value instanceof Number numeric ? numeric.longValue() : Long.parseLong(String.valueOf(value));
            if (number > 0) {
                return number;
            }
        } catch (RuntimeException ignored) {
            // Stable validation error below.
        }
        throw new InvalidProgressionCommandException("GitHub issue number must be positive");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private String asString(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
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
                java.util.List.of(),
                null,
                null,
                null,
                "GITHUB",
                true
        );
    }
}

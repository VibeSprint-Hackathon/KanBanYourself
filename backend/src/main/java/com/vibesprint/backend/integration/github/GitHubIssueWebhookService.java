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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubIssueWebhookService {

    private final QuestRepository questRepository;
    private final ProgressionService progressionService;
    private final DemoApiMapper mapper;
    private final Set<String> processedDeliveries = ConcurrentHashMap.newKeySet();
    private final String acceptedRepository;

    public GitHubIssueWebhookService(
            QuestRepository questRepository,
            ProgressionService progressionService,
            DemoApiMapper mapper,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String acceptedRepository
    ) {
        this.questRepository = questRepository;
        this.progressionService = progressionService;
        this.mapper = mapper;
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
        if (!"closed".equalsIgnoreCase(action)) {
            throw new InvalidProgressionCommandException("Only closed GitHub issues are supported");
        }

        String issueUrl = normalizeIssueUrl(issueNode, payload);
        if (issueUrl == null || issueUrl.isBlank()) {
            throw new InvalidProgressionCommandException("GitHub issue URL is required");
        }

        String repositoryFromUrl = repositoryFromIssueUrl(issueUrl);
        if (repositoryFromUrl == null || !repositoryFromUrl.equalsIgnoreCase(acceptedRepository)) {
            throw new InvalidProgressionCommandException("Only issues from repository " + acceptedRepository + " are accepted");
        }

        Quest linkedQuest = questRepository.findByExternalReference(issueUrl)
                .orElseThrow(() -> new InvalidProgressionCommandException("No quest is linked to GitHub issue " + issueUrl));

        ProgressionResult result = progressionService.completeQuest(
                new CompleteQuestCommand(linkedQuest.getId(), deliveryId, ProgressionSource.GITHUB)
        );
        return mapper.toResponse(result, "GITHUB", false);
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

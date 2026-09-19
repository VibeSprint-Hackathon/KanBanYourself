package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.api.ProgressionResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
public class GitHubIssueWebhookController {

    private final GitHubIssueWebhookService webhookService;

    public GitHubIssueWebhookController(GitHubIssueWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping(value = "/github/issues/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProgressionResponse handle(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody Map<String, Object> payload
    ) {
        return webhookService.process(eventType, deliveryId, payload);
    }
}

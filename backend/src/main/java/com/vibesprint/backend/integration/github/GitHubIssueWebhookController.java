package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.api.ProgressionResponse;
import com.vibesprint.backend.realtime.ProgressionRealtimePublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ProgressionRealtimePublisher realtimePublisher;

    public GitHubIssueWebhookController(
            GitHubIssueWebhookService webhookService,
            ProgressionRealtimePublisher realtimePublisher
    ) {
        this.webhookService = webhookService;
        this.realtimePublisher = realtimePublisher;
    }

    @PostMapping(value = "/github/issues/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handle(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody Map<String, Object> payload
    ) {
        if ("ping".equalsIgnoreCase(eventType)) {
            return ResponseEntity.ok(Map.of("status", "ok"));
        }

        ProgressionResponse response = webhookService.process(eventType, deliveryId, payload);
        realtimePublisher.publish(response);
        if (!response.duplicate()) realtimePublisher.publishBoardChanged();
        return ResponseEntity.ok(response);
    }
}

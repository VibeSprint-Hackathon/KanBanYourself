package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.realtime.ProgressionRealtimePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;

@Service
public class GitHubIssueSyncService {
    private final GitHubIssueService api;
    private final GitHubQuestSyncService sync;
    private final ProgressionRealtimePublisher publisher;
    private final boolean enabled;

    public GitHubIssueSyncService(GitHubIssueService api, GitHubQuestSyncService sync,
            ProgressionRealtimePublisher publisher, @Value("${app.github.token:}") String token,
            @Value("${app.github.enabled:true}") boolean enabled) {
        this.api = api;
        this.sync = sync;
        this.publisher = publisher;
        this.enabled = enabled && !token.isBlank();
    }

    public int syncNow() {
        int count = sync.importIssues(api.fetchAllIssues());
        publisher.publishBoardChanged();
        return count;
    }

    @Scheduled(fixedDelayString = "${app.github.sync-delay-ms:30000}", initialDelay = 10000)
    public void refresh() {
        if (!enabled) return;
        try {
            syncNow();
        } catch (RuntimeException exception) {
            LoggerFactory.getLogger(getClass()).warn("GitHub sync failed: {}", exception.getMessage());
        }
    }
}

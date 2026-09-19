package com.vibesprint.backend.integration.github;

import java.util.List;

public record GitHubIssueResponse(
        long id,
        int number,
        String title,
        String state,
        String url,
        String htmlUrl,
        String repository,
        String body,
        List<String> labels
) {
}

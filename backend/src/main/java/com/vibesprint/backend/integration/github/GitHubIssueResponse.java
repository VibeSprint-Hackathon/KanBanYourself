package com.vibesprint.backend.integration.github;

public record GitHubIssueResponse(
        long id,
        int number,
        String title,
        String state,
        String url,
        String htmlUrl,
        String repository,
        String body
) {
}

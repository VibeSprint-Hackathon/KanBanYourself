package com.vibesprint.backend.integration.github;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GitHubIssueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubIssueService.class);

    private final RestTemplate githubRestTemplate;
    private final String repository;
    private final String apiBaseUrl;
    private final String token;

    public GitHubIssueService(
            RestTemplate githubRestTemplate,
            @Value("${app.github.repository:VibeSprint-Hackathon/KanBanYourself}") String repository,
            @Value("${app.github.api-base-url:https://api.github.com}") String apiBaseUrl,
            @Value("${app.github.token:}") String token
    ) {
        this.githubRestTemplate = githubRestTemplate;
        this.repository = repository;
        this.token = token;
        this.apiBaseUrl = apiBaseUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logLiveIssuesOnStartup() {
        try {
            List<GitHubIssueResponse> issues = fetchIssues();
            LOGGER.info("GitHub integration active for repository {}. Loaded {} issue(s).", repository, issues.size());
            issues.stream().limit(10).forEach(issue ->
                    LOGGER.info("GitHub issue #{}: {} [{}] {}", issue.number(), issue.title(), issue.state(), issue.htmlUrl()));
        } catch (IllegalStateException ex) {
            LOGGER.warn("GitHub live issue sync is unavailable for repository {}: {}", repository, ex.getMessage());
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
                        asString(issueMap.get("body"))
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
        }
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

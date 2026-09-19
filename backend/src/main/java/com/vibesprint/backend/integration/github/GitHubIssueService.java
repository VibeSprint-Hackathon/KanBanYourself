package com.vibesprint.backend.integration.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GitHubIssueService {

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
        this.apiBaseUrl = apiBaseUrl;
        this.token = token;
    }

    public List<GitHubIssueResponse> fetchIssues() {
        return fetchPage("open", 1, false);
    }

    public List<GitHubIssueResponse> fetchAllIssues() {
        List<GitHubIssueResponse> issues = new ArrayList<>();
        // An empty raw page, not the filtered issue count, ends pagination (PRs share this API).
        for (int page = 1; ; page++) {
            List<GitHubIssueResponse> batch = fetchPage("all", page, true);
            if (batch == null) return issues;
            issues.addAll(batch);
        }
    }

    @SuppressWarnings("unchecked")
    private List<GitHubIssueResponse> fetchPage(String state, int page, boolean paginate) {
        String issueUrl = apiBaseUrl + "/repos/" + repository + "/issues?state=" + state + "&per_page=100"
                + (paginate ? "&page=" + page : "");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        headers.set("User-Agent", "VibeSprint-Backend");
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }

        try {
            ResponseEntity<List> response = githubRestTemplate.exchange(
                    issueUrl,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    List.class
            );
            List<Map<String, Object>> payload = response.getBody() == null ? List.of() : response.getBody();
            if (paginate && payload.isEmpty()) return null;
            List<GitHubIssueResponse> issues = new ArrayList<>();
            for (Map<String, Object> issue : payload) {
                if (issue.containsKey("pull_request")) {
                    continue;
                }
                Map<String, Object> assignee = asMap(issue.get("assignee"));
                issues.add(new GitHubIssueResponse(
                        asLong(issue.get("id")),
                        asInt(issue.get("number")),
                        asString(issue.get("title")),
                        asString(issue.get("state")),
                        asString(issue.get("url")),
                        asString(issue.get("html_url")),
                        repository,
                        asString(issue.get("body")),
                        assignee == null ? null : asString(assignee.get("login")),
                        extractLabelNames(issue.get("labels"))
                ));
            }
            return issues;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new IllegalStateException(
                        "GitHub repository not found or inaccessible: " + repository,
                        exception
                );
            }
            throw new IllegalStateException("GitHub API request failed: " + exception.getMessage(), exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "GitHub is unreachable: " + exception.getMessage(),
                    exception
            );
        }
    }

    private List<String> extractLabelNames(Object value) {
        if (!(value instanceof List<?> labels)) {
            return List.of();
        }
        return labels.stream()
                .map(this::asMap)
                .filter(java.util.Objects::nonNull)
                .map(label -> asString(label.get("name")))
                .filter(java.util.Objects::nonNull)
                .filter(label -> !label.isBlank())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }
}

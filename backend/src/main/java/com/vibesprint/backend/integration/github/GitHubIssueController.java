package com.vibesprint.backend.integration.github;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
public class GitHubIssueController {

    private final GitHubIssueService githubIssueService;

    public GitHubIssueController(GitHubIssueService githubIssueService) {
        this.githubIssueService = githubIssueService;
    }

    @GetMapping("/github/issues")
    public List<GitHubIssueResponse> listIssueSummaries() {
        try {
            return githubIssueService.fetchIssues();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage(), ex);
        }
    }
}

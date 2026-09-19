package com.vibesprint.backend.integration.github;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHubIssueServiceTests {

    @Test
    void wrapsNetworkFailureAsIllegalState() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(
                eq("https://api.github.com/repos/VibeSprint-Hackathon/KanBanYourself/issues?state=open&per_page=100"),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(java.util.List.class)
        )).thenThrow(new ResourceAccessException("api.github.com"));

        GitHubIssueService service = new GitHubIssueService(
                restTemplate,
                "VibeSprint-Hackathon/KanBanYourself",
                "https://api.github.com",
                "token"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::fetchIssues);
        org.junit.jupiter.api.Assertions.assertEquals("GitHub is unreachable: api.github.com", ex.getMessage());
    }
}

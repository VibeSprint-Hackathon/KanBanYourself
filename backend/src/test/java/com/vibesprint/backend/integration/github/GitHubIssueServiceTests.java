package com.vibesprint.backend.integration.github;

import com.vibesprint.backend.player.PlayerRepository;
import com.vibesprint.backend.quest.QuestRepository;
import com.vibesprint.backend.quest.QuestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHubIssueServiceTests {

    @Test
    void mapsIssueLabelsToQuestStatuses() {
        assertEquals(QuestStatus.TODO, GitHubIssueService.mapStatus(List.of("todo")));
        assertEquals(QuestStatus.TODO, GitHubIssueService.mapStatus(List.of("ToDo")));
        assertEquals(QuestStatus.IN_PROGRESS, GitHubIssueService.mapStatus(List.of("in progress")));
        assertEquals(QuestStatus.IN_PROGRESS, GitHubIssueService.mapStatus(List.of("in-progress")));
        assertEquals(QuestStatus.IN_PROGRESS, GitHubIssueService.mapStatus(List.of("in_progress")));
        assertEquals(QuestStatus.DONE, GitHubIssueService.mapStatus(List.of("done")));
        assertEquals(QuestStatus.DONE, GitHubIssueService.mapStatus(List.of("Done", "release")));
        assertEquals(QuestStatus.TODO, GitHubIssueService.mapStatus(List.of("urgent", "todo")));
    }

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
                mock(QuestRepository.class),
                mock(PlayerRepository.class),
                "VibeSprint-Hackathon/KanBanYourself",
                "https://api.github.com",
                "token"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::fetchIssues);
        assertEquals("GitHub is unreachable from this environment: api.github.com. Check DNS/network access and proxy settings.", ex.getMessage());
    }
}

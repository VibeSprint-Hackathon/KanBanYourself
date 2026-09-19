package com.vibesprint.backend.api;

import com.vibesprint.backend.integration.DemoResetService;
import com.vibesprint.backend.progression.CompleteQuestCommand;
import com.vibesprint.backend.progression.ProgressionService;
import com.vibesprint.backend.progression.ProgressionSource;
import com.vibesprint.backend.realtime.ProgressionRealtimePublisher;
import com.vibesprint.backend.quest.QuestMutationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoStateQueryService stateQueryService;
    private final ProgressionService progressionService;
    private final DemoApiMapper mapper;
    private final ProgressionRealtimePublisher realtimePublisher;
    private final DemoResetService resetService;
    private final QuestMutationService questMutationService;

    public DemoController(
            DemoStateQueryService stateQueryService,
            ProgressionService progressionService,
            DemoApiMapper mapper,
            ProgressionRealtimePublisher realtimePublisher,
            DemoResetService resetService,
            QuestMutationService questMutationService
    ) {
        this.stateQueryService = stateQueryService;
        this.progressionService = progressionService;
        this.mapper = mapper;
        this.realtimePublisher = realtimePublisher;
        this.resetService = resetService;
        this.questMutationService = questMutationService;
    }

    @GetMapping("/state")
    public DemoStateResponse getState() {
        return stateQueryService.getState();
    }

    @PostMapping("/reset")
    public DemoStateResponse reset() {
        return resetService.reset();
    }

    @PostMapping("/quests")
    public org.springframework.http.ResponseEntity<DemoStateResponse> createQuest(
            @Valid @RequestBody CreateQuestRequest request
    ) {
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(questMutationService.create(request));
    }

    @PutMapping("/quests/{questId}")
    public DemoStateResponse updateQuest(
            @PathVariable @Positive(message = "Quest id must be positive") long questId,
            @Valid @RequestBody UpdateQuestRequest request
    ) {
        return questMutationService.update(questId, request);
    }

    @PatchMapping("/quests/{questId}/move")
    public DemoStateResponse moveQuest(
            @PathVariable @Positive(message = "Quest id must be positive") long questId,
            @Valid @RequestBody MoveQuestRequest request
    ) {
        return questMutationService.move(questId, request);
    }

    @DeleteMapping("/quests/{questId}")
    public DemoStateResponse deleteQuest(
            @PathVariable @Positive(message = "Quest id must be positive") long questId
    ) {
        return questMutationService.delete(questId);
    }

    @PostMapping("/quests/{questId}/complete")
    public ProgressionResponse completeQuest(
            @PathVariable @Positive(message = "Quest id must be positive") long questId,
            @Valid @RequestBody CompleteQuestRequest request
    ) {
        CompleteQuestCommand command = new CompleteQuestCommand(
                questId,
                request.eventId(),
                ProgressionSource.valueOf(request.source().name())
        );
        ProgressionResponse response = mapper.toResponse(progressionService.completeQuest(command));
        realtimePublisher.publish(response);
        return response;
    }
}

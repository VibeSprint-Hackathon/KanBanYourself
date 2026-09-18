package com.vibesprint.backend.api;

import com.vibesprint.backend.integration.DemoResetService;
import com.vibesprint.backend.progression.CompleteQuestCommand;
import com.vibesprint.backend.progression.ProgressionService;
import com.vibesprint.backend.progression.ProgressionSource;
import com.vibesprint.backend.realtime.ProgressionRealtimePublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public DemoController(
            DemoStateQueryService stateQueryService,
            ProgressionService progressionService,
            DemoApiMapper mapper,
            ProgressionRealtimePublisher realtimePublisher,
            DemoResetService resetService
    ) {
        this.stateQueryService = stateQueryService;
        this.progressionService = progressionService;
        this.mapper = mapper;
        this.realtimePublisher = realtimePublisher;
        this.resetService = resetService;
    }

    @GetMapping("/state")
    public DemoStateResponse getState() {
        return stateQueryService.getState();
    }

    @PostMapping("/reset")
    public DemoStateResponse reset() {
        return resetService.reset();
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

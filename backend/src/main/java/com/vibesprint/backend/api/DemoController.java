package com.vibesprint.backend.api;

import com.vibesprint.backend.progression.CompleteQuestCommand;
import com.vibesprint.backend.progression.ProgressionService;
import com.vibesprint.backend.progression.ProgressionSource;
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

    public DemoController(
            DemoStateQueryService stateQueryService,
            ProgressionService progressionService,
            DemoApiMapper mapper
    ) {
        this.stateQueryService = stateQueryService;
        this.progressionService = progressionService;
        this.mapper = mapper;
    }

    @GetMapping("/state")
    public DemoStateResponse getState() {
        return stateQueryService.getState();
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
        return mapper.toResponse(progressionService.completeQuest(command));
    }
}

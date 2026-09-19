package com.vibesprint.backend.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final DemoStateQueryService stateQueryService;

    public PlayerController(DemoStateQueryService stateQueryService) {
        this.stateQueryService = stateQueryService;
    }

    @GetMapping
    public List<DemoStateResponse.PlayerView> getPlayers() {
        return stateQueryService.getPlayers();
    }
}

package com.vibesprint.backend.raid;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/raids")
public class RaidController {

    private final RaidService raidService;

    public RaidController(RaidService raidService) {
        this.raidService = raidService;
    }

    @GetMapping
    public List<RaidResponse> findAll() {
        return raidService.findAll();
    }

    @GetMapping("/{raidId}")
    public RaidResponse find(@PathVariable @Positive long raidId) {
        return raidService.find(raidId);
    }

    @PostMapping
    public ResponseEntity<RaidResponse> create(@Valid @RequestBody CreateRaidRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raidService.create(request));
    }

    @PutMapping("/{raidId}")
    public RaidResponse update(
            @PathVariable @Positive long raidId,
            @Valid @RequestBody UpdateRaidRequest request
    ) {
        return raidService.update(raidId, request);
    }

    @PostMapping("/{raidId}/activate")
    public RaidResponse activate(@PathVariable @Positive long raidId) {
        return raidService.activate(raidId);
    }

    @PostMapping("/{raidId}/cancel")
    public RaidResponse cancel(@PathVariable @Positive long raidId) {
        return raidService.cancel(raidId);
    }

    @PostMapping("/{raidId}/complete")
    public RaidResponse complete(@PathVariable @Positive long raidId) {
        return raidService.complete(raidId);
    }

    @DeleteMapping("/{raidId}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long raidId) {
        raidService.delete(raidId);
        return ResponseEntity.noContent().build();
    }
}

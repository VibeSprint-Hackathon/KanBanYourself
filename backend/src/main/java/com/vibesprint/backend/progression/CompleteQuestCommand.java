package com.vibesprint.backend.progression;

public record CompleteQuestCommand(long questId, String eventId, ProgressionSource source) {

    public CompleteQuestCommand {
        if (questId <= 0) {
            throw new InvalidProgressionCommandException("Quest id must be positive");
        }
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidProgressionCommandException("Event id must not be blank");
        }
        if (source == null) {
            throw new InvalidProgressionCommandException("Progression source is required");
        }
    }
}

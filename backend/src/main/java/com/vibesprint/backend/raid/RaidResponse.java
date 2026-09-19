package com.vibesprint.backend.raid;

public record RaidResponse(
        long id,
        String name,
        String description,
        int maxHp,
        int currentHp,
        RaidStatus status,
        String externalReference
) {
    public static RaidResponse from(Raid raid) {
        return new RaidResponse(
                raid.getId(),
                raid.getName(),
                raid.getDescription(),
                raid.getMaxHp(),
                raid.getCurrentHp(),
                raid.getStatus(),
                raid.getExternalReference()
        );
    }
}

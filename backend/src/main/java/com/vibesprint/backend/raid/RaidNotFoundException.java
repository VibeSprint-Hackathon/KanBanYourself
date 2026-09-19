package com.vibesprint.backend.raid;

public class RaidNotFoundException extends RuntimeException {

    public RaidNotFoundException(long raidId) {
        super("Raid " + raidId + " was not found");
    }
}

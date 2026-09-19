package com.vibesprint.backend.player;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(long playerId) {
        super("Player " + playerId + " was not found");
    }
}

package com.vibesprint.backend.quest;

public class InvalidQuestRequestException extends RuntimeException {

    public InvalidQuestRequestException(String message) {
        super(message);
    }
}

package com.vibesprint.backend.progression;

public class QuestNotFoundException extends RuntimeException {

    private final long questId;

    public QuestNotFoundException(long questId) {
        super("Quest " + questId + " was not found");
        this.questId = questId;
    }

    public long getQuestId() {
        return questId;
    }
}

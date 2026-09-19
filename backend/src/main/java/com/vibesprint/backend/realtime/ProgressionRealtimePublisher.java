package com.vibesprint.backend.realtime;

import com.vibesprint.backend.api.ProgressionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProgressionRealtimePublisher {

    public static final String PROGRESSION_TOPIC = "/topic/progression";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionRealtimePublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public ProgressionRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(ProgressionResponse response) {
        if (!response.applied()) {
            return;
        }

        try {
            messagingTemplate.convertAndSend(PROGRESSION_TOPIC, response);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Progression realtime delivery failed for event {}: {}",
                    response.eventId(),
                    exception.getMessage()
            );
            LOGGER.debug("Progression realtime delivery failure", exception);
        }
    }

    public void publishBoardChanged() {
        try {
            messagingTemplate.convertAndSend("/topic/quests", (Object) java.util.Map.of("type", "GITHUB_SYNC"));
        } catch (RuntimeException exception) {
            LOGGER.warn("Quest refresh notification failed: {}", exception.getMessage());
        }
    }
}

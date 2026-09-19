package com.vibesprint.backend.realtime;

import com.vibesprint.backend.api.ProgressionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ProgressionRealtimePublisherTests {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ProgressionRealtimePublisher publisher = new ProgressionRealtimePublisher(messagingTemplate);

    @Test
    void publishesAppliedProgressionToSingleTopic() {
        ProgressionResponse response = response(true, "applied-event");

        publisher.publish(response);

        verify(messagingTemplate).convertAndSend(ProgressionRealtimePublisher.PROGRESSION_TOPIC, response);
    }

    @Test
    void doesNotPublishRepeatedCompletion() {
        ProgressionResponse response = response(false, "repeated-event");

        publisher.publish(response);

        verify(messagingTemplate, never()).convertAndSend(ProgressionRealtimePublisher.PROGRESSION_TOPIC, response);
    }

    @Test
    void doesNotBreakRestFlowWhenDeliveryFails() {
        ProgressionResponse response = response(true, "delivery-failure");
        doThrow(new MessageDeliveryException("broker unavailable"))
                .when(messagingTemplate)
                .convertAndSend(ProgressionRealtimePublisher.PROGRESSION_TOPIC, response);

        assertDoesNotThrow(() -> publisher.publish(response));
    }

    private ProgressionResponse response(boolean applied, String eventId) {
        return new ProgressionResponse(
                eventId,
                applied,
                applied ? null : "ALREADY_COMPLETED",
                applied ? 180 : 0,
                applied ? 180 : 0,
                applied,
                null,
                applied ? "level-up" : null,
                applied,
                java.util.List.of(),
                null,
                null,
                null,
                "DEMO",
                false
        );
    }
}

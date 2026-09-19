package com.vibesprint.backend.realtime;

import com.vibesprint.backend.api.ProgressionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.github.enabled=false")
class RealtimeWebSocketIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ProgressionRealtimePublisher publisher;

    @Test
    void deliversProgressionThroughConfiguredStompEndpoint() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());

        try {
            StompSession session = client.connectAsync(
                    "ws://localhost:" + port + "/ws",
                    new StompSessionHandlerAdapter() {
                    }
            ).get(5, TimeUnit.SECONDS);

            CompletableFuture<ProgressionResponse> received = new CompletableFuture<>();
            session.subscribe(
                    ProgressionRealtimePublisher.PROGRESSION_TOPIC,
                    new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return ProgressionResponse.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            received.complete((ProgressionResponse) payload);
                        }
                    }
            );
            Thread.sleep(250);

            ProgressionResponse response = response();
            publisher.publish(response);

            ProgressionResponse delivered = received.get(5, TimeUnit.SECONDS);
            session.disconnect();

            assertEquals("websocket-event", delivered.eventId());
            assertEquals(180, delivered.xpGained());
        } finally {
            client.stop();
        }
    }

    private ProgressionResponse response() {
        return new ProgressionResponse(
                "websocket-event",
                true,
                null,
                180,
                180,
                true,
                null,
                "level-up",
                true,
                null,
                null,
                null,
                "DEMO",
                false
        );
    }
}

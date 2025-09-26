package com.industrial.digitaltwin.alertanalytics.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
public class ReplayWebSocketHandler implements WebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("Replay WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("Replay WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // Handle incoming messages from clients if needed
        log.debug("Received message from replay session {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error in replay session: {}", session.getId(), exception);
        sessions.remove(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastMessage(Object message) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(objectMapper.writeValueAsString(message)));
                }
            } catch (IOException e) {
                log.error("Error broadcasting message to replay session: {}", session.getId(), e);
            }
        }
    }

    public void broadcastHistoricalState(Object state) {
        try {
            var update = com.industrial.digitaltwin.alertanalytics.model.AlertAnalyticsUpdate.builder()
                    .updateType("HISTORICAL_STATE")
                    .timestamp(java.time.Instant.now())
                    .payload(state)
                    .build();

            broadcastMessage(update);
            log.debug("Broadcasted historical state via replay WebSocket");
        } catch (Exception e) {
            log.error("Error broadcasting historical state via replay WebSocket", e);
        }
    }
}
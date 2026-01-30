package com.example.messenger.websocket;

import com.example.messenger.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Registers and unregisters WebSocket sessions with EventService by user ID.
 * Events are pushed by EventService to participants' sessions; this handler does not handle inbound message types.
 */
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageWebSocketHandler.class);

    private final EventService eventService;

    public MessageWebSocketHandler(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.getUserIdAttr());
        if (userId == null) {
            log.warn("WebSocket session {} has no userId; closing", session.getId());
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception e) {
                log.debug("Error closing session", e);
            }
            return;
        }
        eventService.registerSession(userId, session);
        log.debug("WebSocket session {} registered for user {}", session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.getUserIdAttr());
        if (userId != null) {
            eventService.unregisterSession(userId, session);
        }
    }
}

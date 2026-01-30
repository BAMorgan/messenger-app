package com.example.messenger.service;

import com.example.messenger.domain.Event;
import com.example.messenger.dto.WebSocketMessage;
import com.example.messenger.repository.ConversationParticipantRepository;
import com.example.messenger.repository.EventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates events for conversations and distributes them to connected WebSocket sessions.
 * Tracks sessions by user ID for fanout to conversation participants.
 */
@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ObjectMapper objectMapper;

    /** User ID -> set of WebSocket sessions (thread-safe). */
    private final Map<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public EventService(
            EventRepository eventRepository,
            ConversationParticipantRepository participantRepository,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.objectMapper = objectMapper;
    }

    public void registerSession(Long userId, WebSocketSession session) {
        sessionsByUserId.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregisterSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> set = sessionsByUserId.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessionsByUserId.remove(userId);
            }
        }
    }

    /**
     * Persists an event and pushes it to all WebSocket sessions of conversation participants.
     */
    public void publish(Long conversationId, String type, String payload) {
        Event event = new Event(conversationId, type, payload);
        event = eventRepository.save(event);

        WebSocketMessage msg = new WebSocketMessage(
                event.getId(),
                event.getConversationId(),
                event.getType(),
                event.getPayload(),
                event.getCreatedAt()
        );
        String json;
        try {
            json = objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}", event.getId(), e);
            return;
        }

        List<Long> participantUserIds = participantRepository.findUserIdsByConversationId(conversationId);
        for (Long userId : participantUserIds) {
            Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
            if (sessions != null) {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(json));
                        } catch (IOException e) {
                            log.warn("Failed to send event to session {}: {}", session.getId(), e.getMessage());
                        }
                    }
                }
            }
        }
    }
}

package com.example.messenger.dto;

import java.time.Instant;

/**
 * DTO for events sent over the WebSocket. Includes event ID for resume capability.
 */
public record WebSocketMessage(
        long eventId,
        long conversationId,
        String type,
        String payload,
        Instant createdAt
) {}

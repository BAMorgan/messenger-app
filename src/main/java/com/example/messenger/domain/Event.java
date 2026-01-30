package com.example.messenger.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Persisted event for WebSocket distribution and resume capability.
 * Clients can request events after a given event ID to catch up.
 */
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Event() {}

    public Event(Long conversationId, String type, String payload) {
        this.conversationId = conversationId;
        this.type = type;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.example.messenger.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    private AppUser sender;

    @Column(nullable = false, length = 4096)
    private String body;

    //timestamp of message object creation
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Unique per conversation; used for deduplication. Nullable for backward compatibility. */
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    protected Message() {}

    public Message(Conversation conversation, AppUser sender, String body) {
        this.conversation = conversation;
        this.sender = sender;
        this.body = body;
    }

    public Message(Conversation conversation, AppUser sender, String body, String idempotencyKey) {
        this.conversation = conversation;
        this.sender = sender;
        this.body = body;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public AppUser getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}

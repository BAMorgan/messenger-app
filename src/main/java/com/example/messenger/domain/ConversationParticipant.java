package com.example.messenger.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Join entity for the many-to-many relationship between conversations and users.
 * Tracks when a user joined and their role in the conversation.
 */
@Entity
@Table(name = "conversation_participant")
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role = ParticipantRole.MEMBER;

    protected ConversationParticipant() {}

    public ConversationParticipant(Conversation conversation, AppUser user, ParticipantRole role) {
        this.conversation = conversation;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public AppUser getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public ParticipantRole getRole() {
        return role;
    }
}

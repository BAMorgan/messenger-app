package com.example.messenger.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversation")
public class Conversation {

    /** Maximum participants allowed in a group conversation. */
    public static final int MAX_GROUP_MEMBERS = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type = ConversationType.ONE_TO_ONE;

    /** Display name for group conversations; null for one-to-one. */
    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationParticipant> participants = new ArrayList<>();

    protected Conversation() {}

    public Conversation(ConversationType type, String name) {
        this.type = type;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public ConversationType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ConversationParticipant> getParticipants() {
        return participants;
    }

    /** Adds a participant; call before persisting so the join is saved. */
    public void addParticipant(AppUser user, ParticipantRole role) {
        ConversationParticipant p = new ConversationParticipant(this, user, role);
        participants.add(p);
    }
}

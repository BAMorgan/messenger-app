package com.example.messenger.repository;

import com.example.messenger.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Repository for conversation participants. Used for participant lookups and max-50 validation. */
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    long countByConversationId(Long conversationId);

    /** User IDs of participants in a conversation (for WebSocket fanout). */
    @Query("SELECT p.user.id FROM ConversationParticipant p WHERE p.conversation.id = :conversationId")
    List<Long> findUserIdsByConversationId(Long conversationId);
}

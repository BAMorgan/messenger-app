package com.example.messenger.repository;

import com.example.messenger.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Repository for messages. */
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** Cursor pagination: first page (oldest first). */
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId, Pageable pageable);

    /** Cursor pagination: messages after cursor (oldest first). */
    List<Message> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId, Pageable pageable);

    /** For idempotency: find existing message by conversation and idempotency key. */
    Optional<Message> findByConversationIdAndIdempotencyKey(Long conversationId, String idempotencyKey);
}

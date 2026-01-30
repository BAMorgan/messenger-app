package com.example.messenger.domain;

/**
 * Value type for message deduplication: unique per conversation.
 * Used when checking for existing messages by idempotency key.
 */
public record MessageIdempotencyKey(Long conversationId, String key) {
    public MessageIdempotencyKey {
        if (conversationId == null || key == null || key.isBlank()) {
            throw new IllegalArgumentException("conversationId and key must be non-null and non-blank");
        }
    }
}

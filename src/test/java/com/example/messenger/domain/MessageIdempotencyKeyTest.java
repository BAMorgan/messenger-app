package com.example.messenger.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageIdempotencyKeyTest {

    @Test
    void constructor_acceptsValidValues() {
        MessageIdempotencyKey key = new MessageIdempotencyKey(1L, "abc-123");

        assertEquals(1L, key.conversationId());
        assertEquals("abc-123", key.key());
    }

    @Test
    void constructor_rejectsNullOrBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new MessageIdempotencyKey(null, "k"));
        assertThrows(IllegalArgumentException.class, () -> new MessageIdempotencyKey(1L, null));
        assertThrows(IllegalArgumentException.class, () -> new MessageIdempotencyKey(1L, " "));
    }
}

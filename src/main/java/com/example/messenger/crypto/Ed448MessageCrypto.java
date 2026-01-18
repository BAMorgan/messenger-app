package com.example.messenger.crypto;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

    // Placeholder Ed448 implementation so the app has a real slot for future crypto logic.
@Component
public class Ed448MessageCrypto implements MessageCrypto {

    // Simple version tag so you can evolve the wire format later without guessing.
    private static final String PREFIX = "ED448v1|";

    // This method is where you'll turn plaintext into ciphertext for storage.
    @Override
    public String encrypt(Long conversationId, String plaintext) {
        // TODO: Use conversationId to derive or look up the right key material.
        // TODO: Run Ed448-based encryption/signing and return a compact payload string.
        // For now, we build a "wire format" that still round-trips to the plaintext.

        // Encode the message bytes so it stays ASCII-safe in storage.
        String dataB64 = Base64.getEncoder().encodeToString(
                plaintext.getBytes(StandardCharsets.UTF_8)
        );

        // Include basic metadata you can parse later (version, conversation id, timestamp).
        String meta = "conv=" + conversationId + "|ts=" + Instant.now().toEpochMilli();

        // Final format: ED448v1|conv=123|ts=1700000000000|data=<base64>
        return PREFIX + meta + "|data=" + dataB64;
    }

    // This method is where you'll turn stored ciphertext back into readable text.
    @Override
    public String decrypt(Long conversationId, String ciphertext) {
        // TODO: Validate/verify the ciphertext (and signature if you add one).
        // TODO: Use conversationId to pick the correct key and decrypt the payload.
        // For now, parse our simple format and decode the original plaintext.

        // If this isn't our format, just pass it through unchanged.
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            return ciphertext;
        }

        // Quick sanity checks so we fail fast on malformed payloads.
        if (!isValidPayload(conversationId, ciphertext)) {
            return ciphertext;
        }

        // Find the "data=" section and decode it back to UTF-8.
        int dataIndex = ciphertext.indexOf("|data=");
        if (dataIndex < 0) {
            return ciphertext;
        }

        String dataB64 = ciphertext.substring(dataIndex + "|data=".length());
        byte[] decoded = Base64.getDecoder().decode(dataB64);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    // Minimal validator to prove the format is parseable before real crypto arrives.
    private boolean isValidPayload(Long conversationId, String payload) {
        // Must have the basic segments we expect.
        if (!payload.contains("|data=") || !payload.contains("|ts=") || !payload.contains("conv=")) {
            return false;
        }

        // Parse the conversation id from the payload and make sure it matches.
        Long parsedConversationId = parseLongField(payload, "conv=");
        if (parsedConversationId == null || !parsedConversationId.equals(conversationId)) {
            return false;
        }

        // Parse the timestamp and make sure it's non-negative and not too far in the future.
        Long timestamp = parseLongField(payload, "ts=");
        if (timestamp == null || timestamp < 0) {
            return false;
        }
        long now = Instant.now().toEpochMilli();
        if (timestamp > now + 5 * 60 * 1000) { // allow small clock skew
            return false;
        }

        // Verify the base64 block is decodable (we don't use the bytes here).
        String dataB64 = payload.substring(payload.indexOf("|data=") + "|data=".length());
        try {
            Base64.getDecoder().decode(dataB64);
        } catch (IllegalArgumentException ex) {
            return false;
        }

        return true;
    }

    // Helper to grab a numeric field like "conv=123" or "ts=1700" from the payload.
    private Long parseLongField(String payload, String key) {
        int start = payload.indexOf(key);
        if (start < 0) {
            return null;
        }
        int valueStart = start + key.length();
        int end = payload.indexOf('|', valueStart);
        String raw = (end < 0) ? payload.substring(valueStart) : payload.substring(valueStart, end);
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

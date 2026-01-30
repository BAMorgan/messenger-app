package com.example.messenger;

import com.example.messenger.domain.AppUser;
import com.example.messenger.domain.Conversation;
import com.example.messenger.domain.ConversationType;
import com.example.messenger.domain.ParticipantRole;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Factory for creating test data objects.
 * 
 * <p>Provides consistent test data creation across all test types.
 * Use this factory to create domain objects with sensible defaults
 * that can be customized as needed.
 * 
 * <p>Usage:
 * <pre>{@code
 * AppUser user = TestDataFactory.createUser("alice");
 * Conversation conv = TestDataFactory.createConversation(user1, user2);
 * Message msg = TestDataFactory.createMessage(conv, user, "Hello!");
 * }</pre>
 */
public final class TestDataFactory {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private TestDataFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a test user with the given username.
     * 
     * @param username the username for the test user
     * @return a new AppUser instance
     */
    public static AppUser createUser(String username) {
        // Generate a test email and password hash for the user
        String email = username + "@test.example.com";
        String passwordHash = "testPasswordHash";
        return new AppUser(username, email, passwordHash);
    }

    /**
     * Creates a test user with a random username.
     * 
     * @return a new AppUser instance with random username
     */
    public static AppUser createRandomUser() {
        return createUser("user_" + UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Generates a unique idempotency key for message testing.
     * 
     * @return a unique string suitable for use as an idempotency key
     */
    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a unique conversation ID for testing.
     *
     * @return a unique long ID
     */
    public static Long generateConversationId() {
        return ID_GENERATOR.getAndIncrement();
    }

    /**
     * Creates an in-memory one-to-one conversation with two participants.
     * Does not persist; for building test data in unit tests.
     *
     * @param userA first participant
     * @param userB second participant
     * @return a new Conversation with two participants
     */
    public static Conversation createConversation(AppUser userA, AppUser userB) {
        Conversation conv = new Conversation(ConversationType.ONE_TO_ONE, null);
        conv.addParticipant(userA, ParticipantRole.MEMBER);
        conv.addParticipant(userB, ParticipantRole.MEMBER);
        return conv;
    }
}

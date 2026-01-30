package com.example.messenger.service;

import com.example.messenger.domain.AppUser;
import com.example.messenger.domain.Conversation;
import com.example.messenger.domain.ConversationType;
import com.example.messenger.domain.ParticipantRole;
import com.example.messenger.repository.AppUserRepository;
import com.example.messenger.TestDataFactory;
import com.example.messenger.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageService with Phase 1.3 multi-user domain model.
 * Verifies createConversation produces ONE_TO_ONE with participants and
 * listMessages includes sender info in MessageView.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void createConversation_createsOneToOneWithTwoParticipants() {
        AppUser userA = appUserRepository.save(TestDataFactory.createUser("alice"));
        AppUser userB = appUserRepository.save(TestDataFactory.createUser("bob"));

        Conversation conv = messageService.createConversation(userA.getId(), userB.getId());

        assertNotNull(conv.getId());
        assertEquals(ConversationType.ONE_TO_ONE, conv.getType());
        assertNull(conv.getName());
        assertNotNull(conv.getCreatedAt());
        assertEquals(2, conv.getParticipants().size());
        assertTrue(conv.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(userA.getId())));
        assertTrue(conv.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(userB.getId())));
    }

    @Test
    void listMessages_includesSenderInfoInMessageView() {
        AppUser userA = appUserRepository.save(TestDataFactory.createUser("alice"));
        AppUser userB = appUserRepository.save(TestDataFactory.createUser("bob"));
        Conversation conv = messageService.createConversation(userA.getId(), userB.getId());

        messageService.sendMessage(conv.getId(), userA.getId(), "Hello from alice");

        List<MessageService.MessageView> views = messageService.listMessages(conv.getId());
        assertEquals(1, views.size());
        MessageService.MessageView v = views.get(0);
        assertEquals(userA.getId(), v.senderId());
        assertEquals("alice", v.senderUsername());
        assertEquals("Hello from alice", v.body());
        assertNotNull(v.createdAt());
    }

    @Test
    void sendMessage_withIdempotencyKey_returnsExistingMessageOnDuplicateKey() {
        AppUser userA = appUserRepository.save(TestDataFactory.createUser("alice"));
        AppUser userB = appUserRepository.save(TestDataFactory.createUser("bob"));
        Conversation conv = messageService.createConversation(userA.getId(), userB.getId());
        String idempotencyKey = TestDataFactory.generateIdempotencyKey();

        var first = messageService.sendMessage(conv.getId(), userA.getId(), "Hello", idempotencyKey);
        var second = messageService.sendMessage(conv.getId(), userA.getId(), "Hello", idempotencyKey);

        assertEquals(first.getId(), second.getId(), "Same idempotency key should return same message");
        assertEquals(1, messageService.listMessages(conv.getId()).size());
    }

    @Test
    void addParticipantToConversation_throwsWhenGroupAlreadyHasMaxMembers() {
        AppUser owner = appUserRepository.save(TestDataFactory.createUser("owner"));
        Conversation group = messageService.createGroupConversation("Test Group", owner.getId());
        for (int i = 0; i < Conversation.MAX_GROUP_MEMBERS - 1; i++) {
            AppUser u = appUserRepository.save(TestDataFactory.createRandomUser());
            messageService.addParticipantToConversation(group.getId(), u.getId(), ParticipantRole.MEMBER);
        }
        AppUser extra = appUserRepository.save(TestDataFactory.createRandomUser());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                messageService.addParticipantToConversation(group.getId(), extra.getId(), ParticipantRole.MEMBER));

        assertTrue(ex.getMessage().contains(String.valueOf(Conversation.MAX_GROUP_MEMBERS)));
    }

    // --- Phase 1.4: Cursor pagination ---

    @Test
    void listMessages_withCursorAndLimit_returnsPageWithNextCursorWhenMoreExist() {
        AppUser userA = appUserRepository.save(TestDataFactory.createUser("alice"));
        AppUser userB = appUserRepository.save(TestDataFactory.createUser("bob"));
        Conversation conv = messageService.createConversation(userA.getId(), userB.getId());
        for (int i = 0; i < 5; i++) {
            messageService.sendMessage(conv.getId(), userA.getId(), "msg-" + i);
        }

        MessageService.MessageListPage first = messageService.listMessages(conv.getId(), null, 2);
        assertEquals(2, first.messages().size());
        assertNotNull(first.nextCursor());

        MessageService.MessageListPage next = messageService.listMessages(conv.getId(), first.nextCursor(), 2);
        assertEquals(2, next.messages().size());
        assertNotNull(next.nextCursor());

        MessageService.MessageListPage last = messageService.listMessages(conv.getId(), next.nextCursor(), 2);
        assertEquals(1, last.messages().size());
        assertNull(last.nextCursor());
    }

    @Test
    void listMessages_withCursorAndLimit_respectsLimit() {
        AppUser userA = appUserRepository.save(TestDataFactory.createUser("alice"));
        AppUser userB = appUserRepository.save(TestDataFactory.createUser("bob"));
        Conversation conv = messageService.createConversation(userA.getId(), userB.getId());
        messageService.sendMessage(conv.getId(), userA.getId(), "one");
        messageService.sendMessage(conv.getId(), userA.getId(), "two");
        messageService.sendMessage(conv.getId(), userA.getId(), "three");

        MessageService.MessageListPage page = messageService.listMessages(conv.getId(), null, 2);
        assertEquals(2, page.messages().size());
    }
}

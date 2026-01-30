package com.example.messenger.service;

import com.example.messenger.domain.AppUser;
import com.example.messenger.domain.Conversation;
import com.example.messenger.domain.ConversationType;
import com.example.messenger.domain.ParticipantRole;
import com.example.messenger.repository.ConversationRepository;
import com.example.messenger.repository.EventRepository;
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
 * Phase 1.4: EventService persists events and does not throw when no WebSocket sessions are registered.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Test
    void publish_createsEventInDatabase() {
        AppUser userA = appUserRepository.save(TestDataFactory.createUser("alice"));
        AppUser userB = appUserRepository.save(TestDataFactory.createUser("bob"));
        Conversation conv = new Conversation(ConversationType.ONE_TO_ONE, null);
        conv.addParticipant(userA, ParticipantRole.MEMBER);
        conv.addParticipant(userB, ParticipantRole.MEMBER);
        conv = conversationRepository.save(conv);

        eventService.publish(conv.getId(), "message", "{\"id\":1,\"body\":\"hi\"}");

        List<com.example.messenger.domain.Event> events = eventRepository.findAll();
        assertEquals(1, events.size());
        assertEquals(conv.getId(), events.get(0).getConversationId());
        assertEquals("message", events.get(0).getType());
        assertEquals("{\"id\":1,\"body\":\"hi\"}", events.get(0).getPayload());
    }
}

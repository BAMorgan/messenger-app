package com.example.messenger.service;


import com.example.messenger.crypto.MessageCrypto;
import com.example.messenger.domain.*;
import com.example.messenger.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service //"business logic"
public class MessageService {

    private final AppUserRepository users;
    private final MessageCrypto crypto;
    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participantRepository;
    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public MessageService(
            AppUserRepository users,
            MessageCrypto crypto,
            MessageRepository messages,
            ConversationRepository conversations,
            ConversationParticipantRepository participantRepository,
            EventService eventService,
            ObjectMapper objectMapper
    ) {
        this.users = users;
        this.crypto = crypto;
        this.messages = messages;
        this.conversations = conversations;
        this.participantRepository = participantRepository;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    public AppUser createUser(String username) {
        // Legacy method - users should register via AuthService instead
        // This method is kept for backward compatibility but requires email and password
        throw new UnsupportedOperationException(
                "Use AuthService.register() instead. This method requires email and password for authentication."
        );
    }

    /** Creates a one-to-one conversation with two participants. */
    public Conversation createConversation(Long userAId, Long userBId) {
        AppUser a = users.findById(userAId).orElseThrow();
        AppUser b = users.findById(userBId).orElseThrow();
        Conversation conv = new Conversation(ConversationType.ONE_TO_ONE, null);
        conv.addParticipant(a, ParticipantRole.MEMBER);
        conv.addParticipant(b, ParticipantRole.MEMBER);
        return conversations.save(conv);
    }

    /** Creates a group conversation with the given name and owner. */
    public Conversation createGroupConversation(String name, Long ownerId) {
        AppUser owner = users.findById(ownerId).orElseThrow();
        Conversation conv = new Conversation(ConversationType.GROUP, name);
        conv.addParticipant(owner, ParticipantRole.OWNER);
        return conversations.save(conv);
    }

    /**
     * Adds a participant to a conversation. For group conversations, enforces max {@value Conversation#MAX_GROUP_MEMBERS} members.
     * @throws IllegalStateException if the conversation is a group and already has the maximum number of participants
     */
    public Conversation addParticipantToConversation(Long conversationId, Long userId, ParticipantRole role) {
        Conversation conversation = conversations.findById(conversationId).orElseThrow();
        AppUser user = users.findById(userId).orElseThrow();
        if (conversation.getType() == ConversationType.GROUP) {
            long count = participantRepository.countByConversationId(conversationId);
            if (count >= Conversation.MAX_GROUP_MEMBERS) {
                throw new IllegalStateException(
                        "Group conversation already has maximum " + Conversation.MAX_GROUP_MEMBERS + " participants");
            }
        }
        conversation.addParticipant(user, role);
        return conversations.save(conversation);
    }

    public Message sendMessage(Long conversationId, Long senderId, String body) {
        return sendMessage(conversationId, senderId, body, null);
    }

    /**
     * Sends a message, optionally with an idempotency key. If a non-blank key is provided and a message
     * with that key already exists for the conversation, returns the existing message without creating a duplicate.
     */
    public Message sendMessage(Long conversationId, Long senderId, String body, String idempotencyKey) {
        Conversation conversation = conversations.findById(conversationId).orElseThrow();
        AppUser sender = users.findById(senderId).orElseThrow();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = messages.findByConversationIdAndIdempotencyKey(conversationId, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        String encrypted = crypto.encrypt(conversationId, body);
        Message message = idempotencyKey != null && !idempotencyKey.isBlank()
                ? new Message(conversation, sender, encrypted, idempotencyKey)
                : new Message(conversation, sender, encrypted);
        message = messages.save(message);

        MessageView view = new MessageView(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                crypto.decrypt(conversationId, message.getBody()),
                message.getCreatedAt()
        );
        try {
            String payload = objectMapper.writeValueAsString(view);
            eventService.publish(conversation.getId(), "message", payload);
        } catch (JsonProcessingException e) {
            // Log but do not fail the request; message is already saved
            org.slf4j.LoggerFactory.getLogger(MessageService.class).warn("Failed to publish message event: {}", e.getMessage());
        }

        return message;
    }

    public List<MessageView> listMessages(Long conversationId) {
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> new MessageView(
                        m.getId(),
                        m.getSender().getId(),
                        m.getSender().getUsername(),
                        crypto.decrypt(conversationId, m.getBody()),
                        m.getCreatedAt()
                ))
                .toList();
    }

    /**
     * Cursor-paginated message list. Returns messages in ascending order by id (oldest first).
     * @param afterId cursor (exclusive); null for first page
     * @param limit max items per page
     * @return page with messages and nextCursor (id of last message, or null if no more)
     */
    public MessageListPage listMessages(Long conversationId, Long afterId, int limit) {
        List<Message> batch = afterId == null
                ? messages.findByConversationIdOrderByIdAsc(conversationId, PageRequest.of(0, limit))
                : messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(conversationId, afterId, PageRequest.of(0, limit));
        List<MessageView> views = batch.stream()
                .map(m -> new MessageView(
                        m.getId(),
                        m.getSender().getId(),
                        m.getSender().getUsername(),
                        crypto.decrypt(conversationId, m.getBody()),
                        m.getCreatedAt()
                ))
                .toList();
        Long nextCursor = batch.size() < limit ? null : batch.get(batch.size() - 1).getId();
        return new MessageListPage(views, nextCursor);
    }

    /** DTO for message list responses; includes sender info for multi-user chats. */
    public record MessageView(Long id, Long senderId, String senderUsername, String body, Instant createdAt) {}

    /** Cursor-paginated message list response. */
    public record MessageListPage(List<MessageView> messages, Long nextCursor) {}
}

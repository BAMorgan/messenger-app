package com.example.messenger.service;


import com.example.messenger.crypto.MessageCrypto;
import com.example.messenger.domain.*;
import com.example.messenger.dto.ConversationSummary;
import com.example.messenger.dto.CreateConversationRequest;
import com.example.messenger.exception.CustomException;
import com.example.messenger.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service //"business logic"
public class MessageService {

    private final AppUserRepository users;
    private final MessageCrypto crypto;
    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participantRepository;
    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final Counter messagesSentCounter;
    private final Timer messageSendTimer;

    public MessageService(
            AppUserRepository users,
            MessageCrypto crypto,
            MessageRepository messages,
            ConversationRepository conversations,
            ConversationParticipantRepository participantRepository,
            EventService eventService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.users = users;
        this.crypto = crypto;
        this.messages = messages;
        this.conversations = conversations;
        this.participantRepository = participantRepository;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.messagesSentCounter = Counter.builder("messenger.messages.sent")
                .description("Total messages sent")
                .register(meterRegistry);
        this.messageSendTimer = Timer.builder("messenger.messages.send.duration")
                .description("Message send latency")
                .register(meterRegistry);
    }

    public AppUser createUser(String username) {
        // Legacy method - users should register via AuthService instead
        // This method is kept for backward compatibility but requires email and password
        throw new UnsupportedOperationException(
                "Use AuthService.register() instead. This method requires email and password for authentication."
        );
    }

    /**
     * Creates a conversation from API request. ONE_TO_ONE requires exactly 2 participants;
     * GROUP requires 1–50 participants and optional name.
     */
    public Conversation createConversation(CreateConversationRequest request) {
        ConversationType type = request.getType();
        List<Long> ids = request.getParticipantIds();
        if (type == ConversationType.ONE_TO_ONE && (ids == null || ids.size() != 2)) {
            throw new CustomException("ONE_TO_ONE conversation requires exactly 2 participant IDs", HttpStatus.BAD_REQUEST);
        }
        if (type == ConversationType.GROUP && (ids == null || ids.isEmpty())) {
            throw new CustomException("GROUP conversation requires at least one participant", HttpStatus.BAD_REQUEST);
        }
        if (type == ConversationType.ONE_TO_ONE) {
            return createConversation(ids.get(0), ids.get(1));
        }
        String name = request.getName() != null ? request.getName() : "Group";
        Conversation group = createGroupConversation(name, ids.get(0));
        for (int i = 1; i < ids.size(); i++) {
            addParticipantToConversation(group.getId(), ids.get(i), ParticipantRole.MEMBER);
        }
        return conversations.findById(group.getId()).orElseThrow(() ->
                new CustomException("Conversation not found", HttpStatus.NOT_FOUND));
    }

    /** Creates a one-to-one conversation with two participants. */
    public Conversation createConversation(Long userAId, Long userBId) {
        AppUser a = users.findById(userAId).orElseThrow(() ->
                new CustomException("User not found: " + userAId, HttpStatus.NOT_FOUND));
        AppUser b = users.findById(userBId).orElseThrow(() ->
                new CustomException("User not found: " + userBId, HttpStatus.NOT_FOUND));
        Conversation conv = new Conversation(ConversationType.ONE_TO_ONE, null);
        conv.addParticipant(a, ParticipantRole.MEMBER);
        conv.addParticipant(b, ParticipantRole.MEMBER);
        return conversations.save(conv);
    }

    /** Creates a group conversation with the given name and owner. */
    public Conversation createGroupConversation(String name, Long ownerId) {
        AppUser owner = users.findById(ownerId).orElseThrow(() ->
                new CustomException("User not found: " + ownerId, HttpStatus.NOT_FOUND));
        Conversation conv = new Conversation(ConversationType.GROUP, name);
        conv.addParticipant(owner, ParticipantRole.OWNER);
        return conversations.save(conv);
    }

    /**
     * Adds a participant to a conversation. For group conversations, enforces max {@value Conversation#MAX_GROUP_MEMBERS} members.
     * @throws IllegalStateException if the conversation is a group and already has the maximum number of participants
     */
    public Conversation addParticipantToConversation(Long conversationId, Long userId, ParticipantRole role) {
        Conversation conversation = conversations.findById(conversationId).orElseThrow(() ->
                new CustomException("Conversation not found: " + conversationId, HttpStatus.NOT_FOUND));
        AppUser user = users.findById(userId).orElseThrow(() ->
                new CustomException("User not found: " + userId, HttpStatus.NOT_FOUND));
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
        Timer.Sample sample = Timer.start();
        try {
            Conversation conversation = conversations.findById(conversationId).orElseThrow(() ->
                    new CustomException("Conversation not found: " + conversationId, HttpStatus.NOT_FOUND));
            AppUser sender = users.findById(senderId).orElseThrow(() ->
                    new CustomException("User not found: " + senderId, HttpStatus.NOT_FOUND));
            ensureParticipant(conversationId, senderId);

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
            messagesSentCounter.increment();

            MessageView view = new MessageView(
                    message.getId(),
                    message.getSender().getId(),
                    message.getSender().getUsername(),
                    crypto.decrypt(conversationId, message.getBody()),
                    message.getCreatedAt()
            );
            try {
                // Serialize event payload with a string timestamp to avoid runtime mapper/module coupling.
                String payload = objectMapper.writeValueAsString(Map.of(
                        "id", view.id(),
                        "senderId", view.senderId(),
                        "senderUsername", view.senderUsername(),
                        "body", view.body(),
                        "createdAt", view.createdAt().toString()
                ));
                eventService.publish(conversation.getId(), "message", payload);
            } catch (JsonProcessingException e) {
                // Log but do not fail the request; message is already saved
                org.slf4j.LoggerFactory.getLogger(MessageService.class).warn("Failed to publish message event: {}", e.getMessage());
            }

            return message;
        } finally {
            sample.stop(messageSendTimer);
        }
    }

    /** Converts a persisted message to a view with decrypted body (for API response). */
    public MessageView toMessageView(Message message) {
        long cid = message.getConversation().getId();
        return new MessageView(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                crypto.decrypt(cid, message.getBody()),
                message.getCreatedAt()
        );
    }

    public List<MessageView> listMessages(Long conversationId) {
        if (!conversations.existsById(conversationId)) {
            throw new CustomException("Conversation not found: " + conversationId, HttpStatus.NOT_FOUND);
        }
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
        if (!conversations.existsById(conversationId)) {
            throw new CustomException("Conversation not found: " + conversationId, HttpStatus.NOT_FOUND);
        }
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

    /**
     * Cursor-paginated message list with participant authorization check.
     */
    public MessageListPage listMessages(Long conversationId, Long requesterUserId, Long afterId, int limit) {
        if (!conversations.existsById(conversationId)) {
            throw new CustomException("Conversation not found: " + conversationId, HttpStatus.NOT_FOUND);
        }
        ensureParticipant(conversationId, requesterUserId);
        return listMessages(conversationId, afterId, limit);
    }

    /** DTO for message list responses; includes sender info for multi-user chats. */
    public record MessageView(Long id, Long senderId, String senderUsername, String body, Instant createdAt) {}

    /** Cursor-paginated message list response. */
    public record MessageListPage(List<MessageView> messages, Long nextCursor) {}

    /**
     * Lists all conversations for a user. Returns lightweight summaries with participant usernames.
     */
    public List<ConversationSummary> listConversationsForUser(Long userId) {
        List<Conversation> convs = conversations.findByParticipantUserId(userId);
        return convs.stream()
                .map(c -> new ConversationSummary(
                        c.getId(),
                        c.getType(),
                        c.getName(),
                        c.getParticipants().stream()
                                .map(p -> p.getUser().getUsername())
                                .toList()
                ))
                .toList();
    }

    private void ensureParticipant(Long conversationId, Long userId) {
        boolean participant = participantRepository.existsByConversation_IdAndUser_Id(conversationId, userId);
        if (!participant) {
            throw new CustomException("Forbidden: user is not a participant in this conversation", HttpStatus.FORBIDDEN);
        }
    }
}

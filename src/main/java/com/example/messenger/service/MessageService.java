package com.example.messenger.service;


import com.example.messenger.crypto.MessageCrypto;
import com.example.messenger.domain.*;
import com.example.messenger.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service //"business logic"
public class MessageService {

    private final AppUserRepository users;
    private final MessageCrypto crypto;
    private final MessageRepository messages;
    private final ConversationRepository conversations;

    public MessageService(
            AppUserRepository users,
            MessageCrypto crypto,
            MessageRepository messages,
            ConversationRepository conversations
    ) {
        this.users = users;
        this.crypto = crypto;
        this.messages = messages;
        this.conversations = conversations;
    }

    public AppUser createUser(String username) {
        return users.save(new AppUser(username));
    }

    //use 'var' instead of AppUser here? why?
    public Conversation createConversation(Long userAId, Long userBId) {
        AppUser a = users.findById(userAId).orElseThrow();
        AppUser b = users.findById(userBId).orElseThrow();
        return  conversations.save(new Conversation(a,b));
    }

    public Message sendMessage(Long conversationId, Long senderId, String body) {
        Conversation conversation = conversations.findById(conversationId).orElseThrow();
        AppUser sender = users.findById(senderId).orElseThrow();

        String encrypted = crypto.encrypt(conversationId, body);
        return messages.save(new Message(conversation, sender, encrypted));
    }

    public List<MessageView> listMessages(Long conversationId) {
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(m -> new MessageView(
                        m.getId(),
                        crypto.decrypt(conversationId, m.getBody()),
                        m.getCreatedAt()
                ))
                .toList();
    }

    //MessageView = Data Transfer Object for moving data between layers (this is view data only)
    public record MessageView(Long id, String body, Instant createdAt) {}
}

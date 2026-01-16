package com.example.messenger.repository;

import com.example.messenger.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//find messages where conversation.id = ??
public interface MessageRepository extends JpaRepository<Message,Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}

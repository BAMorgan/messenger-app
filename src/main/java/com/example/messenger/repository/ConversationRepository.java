package com.example.messenger.repository;

import com.example.messenger.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

//CRUD for conversations
public interface ConversationRepository extends JpaRepository<Conversation,Long> {}

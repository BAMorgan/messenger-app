package com.example.messenger.repository;

import com.example.messenger.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** CRUD for conversations. */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Find all conversations where the given user is a participant. */
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.user.id = :userId")
    List<Conversation> findByParticipantUserId(@Param("userId") Long userId);
}

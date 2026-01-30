package com.example.messenger.repository;

import com.example.messenger.domain.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repository for events; supports resume via event ID. */
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId, Pageable pageable);
}

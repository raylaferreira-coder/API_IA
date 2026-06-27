package com.project.chat.repository;

import com.project.chat.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(value = "Conversation.withMessages")
    List<Conversation> findBySessionSessionIdOrderByUpdatedAtDesc(String sessionId, Pageable pageable);

    long countBySessionSessionId(String sessionId);
}

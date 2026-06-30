package com.project.chat.repository;

import com.project.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);

    Optional<Message> findTopByConversationIdOrderByTimestampDesc(Long conversationId);

    long countByConversationId(Long conversationId);
}

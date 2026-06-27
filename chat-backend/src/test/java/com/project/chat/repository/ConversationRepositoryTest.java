package com.project.chat.repository;

import com.project.chat.entity.Conversation;
import com.project.chat.entity.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ConversationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConversationRepository conversationRepository;

    @Test
    void findBySessionSessionIdOrderByUpdatedAtDesc_ShouldReturnConversations() {
        Session session = new Session("session-1");
        entityManager.persist(session);

        Conversation conv1 = new Conversation(session, "Mais antiga");
        conv1.setUpdatedAt(LocalDateTime.now().minusMinutes(5));
        entityManager.persist(conv1);
        entityManager.flush();

        Conversation conv2 = new Conversation(session, "Mais recente");
        conv2.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(conv2);
        entityManager.flush();

        List<Conversation> found = conversationRepository
                .findBySessionSessionIdOrderByUpdatedAtDesc("session-1", PageRequest.of(0, 100));

        assertEquals(2, found.size());
        assertEquals("Mais recente", found.get(0).getTitle());
        assertEquals("Mais antiga", found.get(1).getTitle());
    }
}

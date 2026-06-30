package com.project.chat.repository;

import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.entity.MessageRole;
import com.project.chat.entity.Session;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Disabled("Requer PostgreSQL + pgvector (VECTOR type incompatível com H2)")
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void findByConversationIdOrderByTimestampAsc_ShouldReturnMessages() {
        Session session = new Session("session-1");
        entityManager.persist(session);

        Conversation conversation = new Conversation(session, "Teste");
        entityManager.persist(conversation);

        Message msg1 = new Message(conversation, MessageRole.USER, "Olá");
        Message msg2 = new Message(conversation, MessageRole.ASSISTANT, "Oi!");
        entityManager.persist(msg1);
        entityManager.persist(msg2);
        entityManager.flush();

        List<Message> messages = messageRepository
                .findByConversationIdOrderByTimestampAsc(conversation.getId());

        assertEquals(2, messages.size());
        assertEquals(MessageRole.USER, messages.get(0).getRole());
        assertEquals(MessageRole.ASSISTANT, messages.get(1).getRole());
    }

    @Test
    void countByConversationId_ShouldReturnCorrectCount() {
        Session session = new Session("session-1");
        entityManager.persist(session);

        Conversation conversation = new Conversation(session, "Teste");
        entityManager.persist(conversation);

        entityManager.persist(new Message(conversation, MessageRole.USER, "M1"));
        entityManager.persist(new Message(conversation, MessageRole.ASSISTANT, "M2"));
        entityManager.flush();

        long count = messageRepository.countByConversationId(conversation.getId());

        assertEquals(2L, count);
    }
}

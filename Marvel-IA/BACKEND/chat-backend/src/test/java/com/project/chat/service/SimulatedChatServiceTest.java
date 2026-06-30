package com.project.chat.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.entity.MessageRole;
import com.project.chat.entity.Session;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.exception.ValidationException;
import com.project.chat.mapper.MessageMapper;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;

@ExtendWith(MockitoExtension.class)
class SimulatedChatServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private ConversationService conversationService;
    @Mock
    private com.project.chat.repository.AttachmentRepository attachmentRepository;
    @Mock
    private TaskService taskService;

    private SimulatedChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new SimulatedChatService(
                sessionRepository, conversationRepository,
                messageRepository, messageMapper, conversationService,
                attachmentRepository, taskService);
    }

    @Test
    void sendMessage_WithValidRequest_ShouldReturnChatResponse() {
        ChatRequest request = new ChatRequest("session-1", null, "Olá", null);
        Session session = new Session("session-1");
        Conversation conversation = new Conversation(session, "Olá");
        conversation.setId(1L);
        Message userMsg = new Message(conversation, MessageRole.USER, "Olá");
        Message assistantMsg = new Message(conversation, MessageRole.ASSISTANT, "Resposta");

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageMapper.toEntity(any(), any(), any())).thenReturn(userMsg);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(userMsg)
                .thenReturn(assistantMsg);

        ChatResponse response = chatService.sendMessage(request);

        assertNotNull(response);
        verify(sessionRepository).findBySessionId("session-1");
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    void sendMessage_WithEmptyContent_ShouldThrowValidationException() {
        ChatRequest request = new ChatRequest("session-1", null, "   ", null);

        assertThrows(ValidationException.class, () -> chatService.sendMessage(request));
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void sendMessage_WithNonExistentSession_ShouldThrowResourceNotFoundException() {
        ChatRequest request = new ChatRequest("invalid-session", null, "Olá", null);

        when(sessionRepository.findBySessionId("invalid-session")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.sendMessage(request));
    }

    @Test
    void getHistory_ShouldDelegateToConversationService() {
        HistoryResponse expected = mock(HistoryResponse.class);
        when(conversationService.getHistory("session-1")).thenReturn(expected);

        HistoryResponse actual = chatService.getHistory("session-1");

        assertSame(expected, actual);
        verify(conversationService).getHistory("session-1");
    }

    @Test
    void getConversation_ShouldDelegateToConversationService() {
        ConversationResponse expected = mock(ConversationResponse.class);
        when(conversationService.getConversation("session-1", 1L)).thenReturn(expected);

        ConversationResponse actual = chatService.getConversation("session-1", 1L);

        assertSame(expected, actual);
        verify(conversationService).getConversation("session-1", 1L);
    }
}

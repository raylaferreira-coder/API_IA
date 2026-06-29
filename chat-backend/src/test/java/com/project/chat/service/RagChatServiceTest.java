package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.entity.*;
import com.project.chat.exception.LlmServiceException;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.exception.ValidationException;
import com.project.chat.mapper.MessageMapper;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import com.project.chat.service.parser.ParserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

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
    private EmbeddingService embeddingService;
    @Mock
    private RetrievalService retrievalService;
    @Mock
    private PromptBuilder promptBuilder;
    @Mock
    private OllamaChatService ollamaChatService;
    @Mock
    private com.project.chat.repository.AttachmentRepository attachmentRepository;
    @Mock
    private OllamaVisionService ollamaVisionService;
    @Mock
    private ParserFactory parserFactory;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private ChunkService chunkService;
    @Mock
    private WebhookService webhookService;

    private RagChatService ragChatService;

    @BeforeEach
    void setUp() {
        ragChatService = new RagChatService(
                sessionRepository, conversationRepository,
                messageRepository, messageMapper,
                conversationService, embeddingService,
                retrievalService, promptBuilder,
                ollamaChatService, attachmentRepository, 5,
                ollamaVisionService, parserFactory,
                documentRepository, documentChunkRepository,
                chunkService, webhookService);
    }

    @Test
    void sendMessage_WithValidRequest_ShouldReturnChatResponse() {
        ChatRequest request = new ChatRequest("session-1", null, "Quem é o Homem de Ferro?", null);
        Session session = new Session("session-1");
        Conversation conversation = new Conversation(session, "Quem é o Homem de Ferro?");
        conversation.setId(1L);
        Message userMsg = new Message(conversation, MessageRole.USER, "Quem é o Homem de Ferro?");
        Message assistantMsg = new Message(conversation, MessageRole.ASSISTANT, "Tony Stark");

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageMapper.toEntity(any(), any(), any())).thenReturn(userMsg);
        when(messageRepository.save(any(Message.class)))
                .thenReturn(userMsg)
                .thenReturn(assistantMsg);
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        when(retrievalService.search(any(float[].class), anyInt())).thenReturn(List.of());
        when(promptBuilder.buildWithContext(anyString(), anyList())).thenReturn("prompt com contexto");
        when(ollamaChatService.generate(anyString())).thenReturn("Tony Stark");

        ChatResponse response = ragChatService.sendMessage(request);

        assertNotNull(response);
        verify(sessionRepository).findBySessionId("session-1");
        verify(embeddingService).embed(anyString());
        verify(retrievalService).search(any(float[].class), eq(5));
        verify(ollamaChatService).generate(anyString());
        verify(messageRepository, times(2)).save(any(Message.class));
    }

    @Test
    void sendMessage_WithEmptyContent_ShouldThrowValidationException() {
        ChatRequest request = new ChatRequest("session-1", null, "   ", null);

        assertThrows(ValidationException.class, () -> ragChatService.sendMessage(request));
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void sendMessage_WithNonExistentSession_ShouldThrowResourceNotFoundException() {
        ChatRequest request = new ChatRequest("invalid", null, "Olá", null);

        when(sessionRepository.findBySessionId("invalid")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ragChatService.sendMessage(request));
    }

    @Test
    void sendMessage_WhenRagFails_ShouldThrowLlmServiceException() {
        ChatRequest request = new ChatRequest("session-1", null, "Quem é o Thanos?", null);
        Session session = new Session("session-1");

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(new Conversation(session, "teste"));
        when(messageMapper.toEntity(any(), any(), any())).thenReturn(new Message());
        when(messageRepository.save(any(Message.class))).thenReturn(new Message());
        when(promptBuilder.buildWithContext(anyString(), anyList())).thenReturn("prompt");
        when(ollamaChatService.generate(anyString())).thenThrow(new RuntimeException("Ollama down"));

        assertThrows(LlmServiceException.class, () -> ragChatService.sendMessage(request));
    }

    @Test
    void getHistory_ShouldDelegateToConversationService() {
        ragChatService.getHistory("session-1");

        verify(conversationService).getHistory("session-1");
    }

    @Test
    void getConversation_ShouldDelegateToConversationService() {
        ragChatService.getConversation("session-1", 1L);

        verify(conversationService).getConversation("session-1", 1L);
    }
}

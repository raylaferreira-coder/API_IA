package com.project.chat.service;

import com.project.chat.ai.MarvelAiAgent;
import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.*;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.exception.ValidationException;
import com.project.chat.mapper.MessageMapper;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("rag")
public class RagChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);
    private static final int MAX_CONTEXT_CHUNKS = 5;

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final PromptBuilder promptBuilder;
    private final MarvelAiAgent marvelAiAgent;

    public RagChatService(SessionRepository sessionRepository,
                          ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          MessageMapper messageMapper,
                          ConversationService conversationService,
                          EmbeddingService embeddingService,
                          DocumentChunkRepository documentChunkRepository,
                          PromptBuilder promptBuilder,
                          MarvelAiAgent marvelAiAgent) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
        this.promptBuilder = promptBuilder;
        this.marvelAiAgent = marvelAiAgent;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Tentativa de envio de mensagem vazia.");
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }

        Session session = sessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + request.getSessionId()));

        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);

        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversa não encontrada: " + request.getConversationId()));
            if (!conversation.getSession().getSessionId().equals(request.getSessionId())) {
                throw new ResourceNotFoundException(
                        "Conversa não encontrada: " + request.getConversationId());
            }
        } else {
            String title = content.length() > 50
                    ? content.substring(0, 50) + "..."
                    : content;
            conversation = new Conversation(session, title);
            conversation = conversationRepository.save(conversation);
            log.info("Nova conversa criada: id={}", conversation.getId());
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Message userMessage = messageMapper.toEntity(request, conversation, MessageRole.USER);
        userMessage = messageRepository.save(userMessage);
        log.info("Mensagem do usuário salva: id={}", userMessage.getId());

        // RAG flow: embedding → retrieval → prompt builder → Ollama → response
        String answer;
        try {
            List<DocumentChunk> chunks = searchRelevantChunks(content);
            String chunkContext = promptBuilder.buildChunkContext(chunks);
            answer = marvelAiAgent.askWithContext(content, chunkContext);
            log.info("Resposta gerada via RAG ({} caracteres)", answer.length());
        } catch (Exception e) {
            log.warn("Fluxo RAG falhou, usando fallback: {}", e.getMessage());
            answer = marvelAiAgent.ask(content);
        }

        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, answer);
        assistantMessage = messageRepository.save(assistantMessage);
        log.info("Resposta do assistente salva: id={}", assistantMessage.getId());

        MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
        MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

        return new ChatResponse(userMsgResponse, assistantMsgResponse, conversation.getId());
    }

    private List<DocumentChunk> searchRelevantChunks(String query) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorStr = toVectorString(queryEmbedding);
            return documentChunkRepository.findSimilarChunks(vectorStr, MAX_CONTEXT_CHUNKS);
        } catch (Exception e) {
            log.warn("Erro ao buscar chunks relevantes: {}", e.getMessage());
            return List.of();
        }
    }

    private static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String sessionId) {
        return conversationService.getHistory(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String sessionId, Long conversationId) {
        return conversationService.getConversation(sessionId, conversationId);
    }
}

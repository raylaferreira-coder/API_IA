package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.*;
import com.project.chat.exception.LlmServiceException;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.exception.SessionConflictException;
import com.project.chat.exception.ValidationException;
import com.project.chat.mapper.MessageMapper;
import com.project.chat.repository.AttachmentRepository;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Profile("rag")
public class RagChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);
    private final int topK;

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final OllamaChatService ollamaChatService;
    private final AttachmentRepository attachmentRepository;

    public RagChatService(SessionRepository sessionRepository,
                          ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          MessageMapper messageMapper,
                          ConversationService conversationService,
                          EmbeddingService embeddingService,
                          RetrievalService retrievalService,
                          PromptBuilder promptBuilder,
                          OllamaChatService ollamaChatService,
                          AttachmentRepository attachmentRepository,
                          @Value("${rag.topK:5}") int topK) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.embeddingService = embeddingService;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.ollamaChatService = ollamaChatService;
        this.attachmentRepository = attachmentRepository;
        this.topK = topK;
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

        if (session.isExpired()) {
            throw new SessionConflictException("Sessão expirada: " + request.getSessionId());
        }

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

        if (request.getAttachmentId() != null) {
            Attachment attachment = attachmentRepository.findById(request.getAttachmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Anexo não encontrado: " + request.getAttachmentId()));
            attachment.setMessage(userMessage);
            attachmentRepository.save(attachment);
            userMessage.setAttachment(attachment);
            log.info("Anexo vinculado à mensagem: attachmentId={}", attachment.getId());
        }

        // RAG flow: embedding → retrieval → prompt builder → Ollama → response
        String answer;
        try {
            float[] questionVector = embeddingService.embed(content);
            List<DocumentChunk> chunks = retrievalService.search(questionVector, topK);
            String finalPrompt = promptBuilder.buildWithContext(content, chunks);
            answer = ollamaChatService.generate(finalPrompt);
            log.info("Resposta gerada via RAG ({} caracteres)", answer.length());
        } catch (Exception e) {
            log.error("Fluxo RAG falhou: {}", e.getMessage(), e);
            throw new LlmServiceException("O serviço de IA local está indisponível. Verifique se o Ollama está em execução.", e);
        }

        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, answer);
        assistantMessage = messageRepository.save(assistantMessage);
        log.info("Resposta do assistente salva: id={}", assistantMessage.getId());

        MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
        MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

        return new ChatResponse(userMsgResponse, assistantMsgResponse, conversation.getId());
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

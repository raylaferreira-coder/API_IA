package com.project.chat.service;

import com.project.chat.ai.MarvelAiAgent;
import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Primary
@Profile({"rag", "prod"})
public class OllamaChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);
    private static final int MAX_RAG_CHUNKS = 5;

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final MarvelAiAgent marvelAiAgent;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;

    public OllamaChatService(SessionRepository sessionRepository,
                             ConversationRepository conversationRepository,
                             MessageRepository messageRepository,
                             MessageMapper messageMapper,
                             ConversationService conversationService,
                             MarvelAiAgent marvelAiAgent,
                             RetrievalService retrievalService,
                             PromptBuilder promptBuilder) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.marvelAiAgent = marvelAiAgent;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
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
        log.info("Mensagem do usuario salva: id={}", userMessage.getId());

        String ragContext = retrievalService.retrieveContext(content, MAX_RAG_CHUNKS);
        String prompt = promptBuilder.buildPrompt(content, ragContext, conversation.getId());
        log.info("Prompt gerado com contexto RAG ({} chars) e historico ({} chars)",
                ragContext.length(), prompt.length());

        String aiResponse;
        try {
            if (!ragContext.isBlank()) {
                aiResponse = marvelAiAgent.askWithContext(content, ragContext);
            } else {
                aiResponse = marvelAiAgent.ask(content);
            }
        } catch (Exception e) {
            log.error("Erro ao chamar MarvelAiAgent: {}", e.getMessage());
            aiResponse = "Desculpe, ocorreu um erro ao processar sua mensagem. " +
                    "Verifique se o Ollama esta disponivel.";
        }

        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, aiResponse);
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

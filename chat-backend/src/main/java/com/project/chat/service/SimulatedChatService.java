package com.project.chat.service;

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
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SimulatedChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedChatService.class);

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;

    public SimulatedChatService(SessionRepository sessionRepository,
                                ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                MessageMapper messageMapper,
                                ConversationService conversationService) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Tentativa de envio de mensagem vazia.");
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }

        if (content.length() > 5000) {
            log.warn("Tentativa de envio de mensagem excede o limite de 5000 caracteres.");
            throw new ValidationException("A mensagem excede o limite de 5000 caracteres.");
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

        Message userMessage = new Message(conversation, MessageRole.USER, content);
        userMessage = messageRepository.save(userMessage);
        log.info("Mensagem do usuário salva: id={}", userMessage.getId());

        String simulatedResponse = generateSimulatedResponse(content);
        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, simulatedResponse);
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

    private String generateSimulatedResponse(String userContent) {
        String lower = userContent.toLowerCase();

        if (lower.contains("olá") || lower.contains("oi") || lower.contains("bom dia")) {
            return "Olá! Como posso ajudar você hoje?";
        }
        if (lower.contains("capital") || lower.contains("brasil")) {
            return "A capital do Brasil é Brasília.";
        }
        if (lower.contains("obrigado") || lower.contains("valeu")) {
            return "Por nada! Estou aqui para ajudar.";
        }
        if (lower.contains("tchau") || lower.contains("até logo")) {
            return "Até logo! Se precisar de algo, é só chamar.";
        }

        return "Entendi sua mensagem. No momento estou em modo de simulação, "
                + "mas em breve poderei processar suas solicitações com inteligência artificial.";
    }

}

package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.request.UploadAndAskRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.*;
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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Profile("dev")
public class SimulatedChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedChatService.class);

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final AttachmentRepository attachmentRepository;

    public SimulatedChatService(SessionRepository sessionRepository,
                                ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                MessageMapper messageMapper,
                                ConversationService conversationService,
                                AttachmentRepository attachmentRepository) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.attachmentRepository = attachmentRepository;
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

        String simulatedResponse = generateSimulatedResponse(content);
        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, simulatedResponse);
        assistantMessage = messageRepository.save(assistantMessage);
        log.info("Resposta do assistente salva: id={}", assistantMessage.getId());

        MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
        MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

        return new ChatResponse(userMsgResponse, assistantMsgResponse, conversation.getId());
    }

    @Override
    @Transactional
    public ChatResponse uploadAndAsk(UploadAndAskRequest request) {
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
        } else {
            conversation = new Conversation(session, "Arquivo: " + request.getOriginalFileName());
            conversation = conversationRepository.save(conversation);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        String question = request.getContent() != null && !request.getContent().trim().isEmpty()
                ? request.getContent()
                : "Analise o arquivo: " + request.getOriginalFileName();

        Message userMessage = new Message(conversation, MessageRole.USER,
                question + "\n\n[Arquivo: " + request.getOriginalFileName() + "]");
        userMessage = messageRepository.save(userMessage);

        String simulatedResponse = "Recebi o arquivo \"" + request.getOriginalFileName()
                + "\". No momento estou em modo de simulacao, "
                + "mas em breve poderei processar arquivos com inteligencia artificial.";

        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, simulatedResponse);
        assistantMessage = messageRepository.save(assistantMessage);

        return new ChatResponse(
                messageMapper.toResponse(userMessage),
                messageMapper.toResponse(assistantMessage),
                conversation.getId());
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

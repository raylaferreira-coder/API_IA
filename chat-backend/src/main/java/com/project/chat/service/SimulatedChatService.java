package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.*;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.exception.ValidationException;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SimulatedChatService implements ChatService {

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public SimulatedChatService(SessionRepository sessionRepository,
                                ConversationRepository conversationRepository,
                                MessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }

        Session session = sessionRepository.findById(request.getSessionId())
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
            conversation = new Conversation(request.getSessionId(), title);
            conversation = conversationRepository.save(conversation);
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Message userMessage = new Message(conversation, MessageRole.USER, content);
        userMessage = messageRepository.save(userMessage);

        String simulatedResponse = generateSimulatedResponse(content);
        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, simulatedResponse);
        assistantMessage = messageRepository.save(assistantMessage);

        MessageResponse userMsgResponse = toMessageResponse(userMessage);
        MessageResponse assistantMsgResponse = toMessageResponse(assistantMessage);

        return new ChatResponse(userMsgResponse, assistantMsgResponse);
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

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole().name(),
                message.getContent(),
                message.getTimestamp()
        );
    }
}

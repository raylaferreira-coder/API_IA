package com.project.chat.service;

import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.ConversationSummaryResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationService {

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(SessionRepository sessionRepository,
                               ConversationRepository conversationRepository,
                               MessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma conversa encontrada para a sessão: " + sessionId));

        List<Conversation> conversations = conversationRepository
                .findBySessionIdOrderByUpdatedAtDesc(sessionId);

        List<ConversationSummaryResponse> summaries = conversations.stream()
                .map(this::toSummary)
                .toList();

        return new HistoryResponse(sessionId, summaries);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String sessionId, Long conversationId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + sessionId));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversa não encontrada: " + conversationId));

        if (!conversation.getSessionId().equals(sessionId)) {
            throw new ResourceNotFoundException("Conversa não encontrada: " + conversationId);
        }

        List<Message> messages = messageRepository
                .findByConversationIdOrderByTimestampAsc(conversationId);

        List<MessageResponse> messageResponses = messages.stream()
                .map(this::toMessageResponse)
                .toList();

        return new ConversationResponse(conversationId, messageResponses);
    }

    private ConversationSummaryResponse toSummary(Conversation conversation) {
        long count = messageRepository.countByConversationId(conversation.getId());
        List<Message> messages = messageRepository
                .findByConversationIdOrderByTimestampAsc(conversation.getId());
        String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();

        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                (int) count,
                lastMessage,
                conversation.getUpdatedAt()
        );
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

package com.project.chat.service;

import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.mapper.ConversationMapper;
import com.project.chat.mapper.MessageMapper;
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
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public ConversationService(SessionRepository sessionRepository,
                               ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               ConversationMapper conversationMapper,
                               MessageMapper messageMapper) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String sessionId) {
        sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma conversa encontrada para a sessão: " + sessionId));

        List<Conversation> conversations = conversationRepository
                .findBySessionSessionIdOrderByUpdatedAtDesc(sessionId);

        List<com.project.chat.dto.response.ConversationSummaryResponse> summaries = conversations.stream()
                .map(conversationMapper::toSummary)
                .toList();

        return new HistoryResponse(sessionId, summaries);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String sessionId, Long conversationId) {
        sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + sessionId));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversa não encontrada: " + conversationId));

        if (!conversation.getSession().getSessionId().equals(sessionId)) {
            throw new ResourceNotFoundException("Conversa não encontrada: " + conversationId);
        }

        List<Message> messages = messageRepository
                .findByConversationIdOrderByTimestampAsc(conversationId);

        return conversationMapper.toResponse(conversation, messages);
    }
}

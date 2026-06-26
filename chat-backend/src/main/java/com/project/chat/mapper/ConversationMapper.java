package com.project.chat.mapper;

import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.ConversationSummaryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.repository.MessageRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationMapper {

    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;

    public ConversationMapper(MessageRepository messageRepository, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
    }

    public ConversationSummaryResponse toSummary(Conversation conversation) {
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

    public ConversationResponse toResponse(Conversation conversation, List<Message> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(messageMapper::toResponse)
                .toList();
        return new ConversationResponse(conversation.getId(), messageResponses);
    }
}

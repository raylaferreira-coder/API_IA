package com.project.chat.mapper;

import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.ConversationSummaryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ConversationMapper {

    private final MessageMapper messageMapper;

    public ConversationMapper(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public ConversationSummaryResponse toSummary(Conversation conversation) {
        List<Message> messages = conversation.getMessages();
        long count = messages.size();
        String lastMessage = messages.stream()
                .max(Comparator.comparing(Message::getTimestamp))
                .map(Message::getContent)
                .orElse("");

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

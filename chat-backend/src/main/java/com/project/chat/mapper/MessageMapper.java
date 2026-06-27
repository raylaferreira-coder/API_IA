package com.project.chat.mapper;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.entity.MessageRole;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole().name(),
                message.getContent(),
                message.getTimestamp(),
                message.getAttachment()
        );
    }

    public Message toEntity(ChatRequest request, Conversation conversation, MessageRole role) {
        return new Message(conversation, role, request.getContent());
    }
}

package com.project.chat.mapper;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Message;
import com.project.chat.entity.MessageRole;
import com.project.chat.repository.AttachmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MessageMapper {

    private final AttachmentRepository attachmentRepository;

    public MessageMapper(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional(readOnly = true)
    public MessageResponse toResponse(Message message) {
        Attachment attachment = null;
        if (message.getAttachment() != null) {
            attachment = message.getAttachment();
        }
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole().name(),
                message.getContent(),
                message.getTimestamp(),
                attachment
        );
    }

    public Message toEntity(ChatRequest request, Conversation conversation, MessageRole role) {
        Message message = new Message(conversation, role, request.getContent());
        if (request.getAttachmentId() != null) {
            attachmentRepository.findById(request.getAttachmentId())
                    .ifPresent(attachment -> {
                        message.setAttachment(attachment);
                        attachment.setMessage(message);
                    });
        }
        return message;
    }
}

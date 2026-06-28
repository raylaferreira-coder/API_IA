package com.project.chat.dto.response;

import java.time.LocalDateTime;

public class MessageResponse {

    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime timestamp;
    private AttachmentResponse attachment;

    public MessageResponse() {
    }

    public MessageResponse(Long id, Long conversationId, String role, String content, LocalDateTime timestamp) {
        this(id, conversationId, role, content, timestamp, null);
    }

    public MessageResponse(Long id, Long conversationId, String role, String content, LocalDateTime timestamp, AttachmentResponse attachment) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.attachment = attachment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AttachmentResponse getAttachment() {
        return attachment;
    }

    public void setAttachment(AttachmentResponse attachment) {
        this.attachment = attachment;
    }
}

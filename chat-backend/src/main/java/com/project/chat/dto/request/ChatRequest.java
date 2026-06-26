package com.project.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    @NotBlank(message = "O identificador de sessão é obrigatório.")
    private String sessionId;

    private Long conversationId;

    @NotBlank(message = "O conteúdo da mensagem não pode estar vazio.")
    @Size(max = 5000, message = "A mensagem excede o limite de 5000 caracteres.")
    private String content;

    private Long attachmentId;

    public ChatRequest() {
    }

    public ChatRequest(String sessionId, Long conversationId, String content, Long attachmentId) {
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.content = content;
        this.attachmentId = attachmentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }
}

package com.project.chat.dto.response;

public class ChatResponse {

    private MessageResponse userMessage;
    private MessageResponse assistantMessage;
    private Long conversationId;

    public ChatResponse() {
    }

    public ChatResponse(MessageResponse userMessage, MessageResponse assistantMessage) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
    }

    public ChatResponse(MessageResponse userMessage, MessageResponse assistantMessage, Long conversationId) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.conversationId = conversationId;
    }

    public MessageResponse getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(MessageResponse userMessage) {
        this.userMessage = userMessage;
    }

    public MessageResponse getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(MessageResponse assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
}

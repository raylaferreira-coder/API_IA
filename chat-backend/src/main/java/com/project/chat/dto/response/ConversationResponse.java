package com.project.chat.dto.response;

import java.util.List;

public class ConversationResponse {

    private Long conversationId;
    private List<MessageResponse> messages;

    public ConversationResponse() {
    }

    public ConversationResponse(Long conversationId, List<MessageResponse> messages) {
        this.conversationId = conversationId;
        this.messages = messages;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }
}

package com.project.chat.dto.response;

import java.util.List;

public class ConversationResponse {

    private Long id;
    private List<MessageResponse> messages;

    public ConversationResponse() {
    }

    public ConversationResponse(Long id, List<MessageResponse> messages) {
        this.id = id;
        this.messages = messages;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }
}

package com.project.chat.dto.response;

import java.util.List;

public class HistoryResponse {

    private String sessionId;
    private List<ConversationSummaryResponse> conversations;

    public HistoryResponse() {
    }

    public HistoryResponse(String sessionId, List<ConversationSummaryResponse> conversations) {
        this.sessionId = sessionId;
        this.conversations = conversations;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ConversationSummaryResponse> getConversations() {
        return conversations;
    }

    public void setConversations(List<ConversationSummaryResponse> conversations) {
        this.conversations = conversations;
    }
}

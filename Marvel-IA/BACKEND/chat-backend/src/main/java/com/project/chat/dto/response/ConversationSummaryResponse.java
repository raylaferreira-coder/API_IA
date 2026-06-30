package com.project.chat.dto.response;

import java.time.LocalDateTime;

public class ConversationSummaryResponse {

    private Long id;
    private String title;
    private int messageCount;
    private String lastMessage;
    private LocalDateTime lastActivity;

    public ConversationSummaryResponse() {
    }

    public ConversationSummaryResponse(Long id, String title, int messageCount, String lastMessage, LocalDateTime lastActivity) {
        this.id = id;
        this.title = title;
        this.messageCount = messageCount;
        this.lastMessage = lastMessage;
        this.lastActivity = lastActivity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}

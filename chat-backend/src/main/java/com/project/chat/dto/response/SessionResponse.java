package com.project.chat.dto.response;

import java.time.LocalDateTime;

public class SessionResponse {

    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private boolean expired;

    public SessionResponse() {
    }

    public SessionResponse(String sessionId, LocalDateTime createdAt, LocalDateTime lastActivity, boolean expired) {
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.lastActivity = lastActivity;
        this.expired = expired;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }
}

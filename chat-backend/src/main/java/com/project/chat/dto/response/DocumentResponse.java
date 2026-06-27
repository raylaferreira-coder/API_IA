package com.project.chat.dto.response;

import com.project.chat.entity.DocumentStatus;
import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String title;
    private String sourceUrl;
    private String sourceType;
    private DocumentStatus status;
    private String errorMessage;
    private int chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {}

    public DocumentResponse(Long id, String title, String sourceUrl, String sourceType,
                            DocumentStatus status, String errorMessage, int chunkCount,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.sourceType = sourceType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.chunkCount = chunkCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

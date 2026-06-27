package com.project.chat.dto.response;

import com.project.chat.entity.DocumentStatus;
import java.time.LocalDateTime;

public class IngestionResponse {

    private Long documentId;
    private String title;
    private DocumentStatus status;
    private int chunkCount;
    private LocalDateTime createdAt;
    private String message;

    public IngestionResponse() {}

    public IngestionResponse(Long documentId, String title, DocumentStatus status,
                             int chunkCount, LocalDateTime createdAt, String message) {
        this.documentId = documentId;
        this.title = title;
        this.status = status;
        this.chunkCount = chunkCount;
        this.createdAt = createdAt;
        this.message = message;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

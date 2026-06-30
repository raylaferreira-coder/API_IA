package com.project.chat.dto.response;

import com.project.chat.entity.DocumentStatus;
import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String fileName;
    private String sourceType;
    private Long fileSize;
    private DocumentStatus status;
    private int totalChunks;
    private LocalDateTime createdAt;

    public DocumentResponse() {}

    public DocumentResponse(Long id, String fileName, String sourceType,
                            Long fileSize, DocumentStatus status, int totalChunks,
                            LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.sourceType = sourceType;
        this.fileSize = fileSize;
        this.status = status;
        this.totalChunks = totalChunks;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

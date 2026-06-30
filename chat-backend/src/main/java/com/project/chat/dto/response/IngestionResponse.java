package com.project.chat.dto.response;

import com.project.chat.entity.DocumentStatus;
import java.time.LocalDateTime;

public class IngestionResponse {

    private Long documentId;
    private String fileName;
    private DocumentStatus status;
    private int chunks;
    private long processingTime;
    private String message;

    public IngestionResponse() {}

    public IngestionResponse(Long documentId, String fileName, DocumentStatus status,
                             int chunks, long processingTime, String message) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.status = status;
        this.chunks = chunks;
        this.processingTime = processingTime;
        this.message = message;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public int getChunks() { return chunks; }
    public void setChunks(int chunks) { this.chunks = chunks; }

    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

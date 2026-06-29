package com.project.chat.dto.request;

public class UploadAndAskRequest {

    private String sessionId;
    private Long conversationId;
    private String content;
    private String storedFilePath;
    private String originalFileName;
    private String contentType;
    private long fileSize;

    public UploadAndAskRequest() {
    }

    public UploadAndAskRequest(String sessionId, Long conversationId, String content,
                                String storedFilePath, String originalFileName,
                                String contentType, long fileSize) {
        this.sessionId = sessionId;
        this.conversationId = conversationId;
        this.content = content;
        this.storedFilePath = storedFilePath;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStoredFilePath() { return storedFilePath; }
    public void setStoredFilePath(String storedFilePath) { this.storedFilePath = storedFilePath; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}

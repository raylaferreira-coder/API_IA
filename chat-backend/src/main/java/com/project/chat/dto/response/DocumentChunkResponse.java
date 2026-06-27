package com.project.chat.dto.response;

public class DocumentChunkResponse {

    private Long id;
    private Long documentId;
    private int chunkIndex;
    private String content;
    private int tokenCount;

    public DocumentChunkResponse() {}

    public DocumentChunkResponse(Long id, Long documentId, int chunkIndex,
                                 String content, int tokenCount) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.tokenCount = tokenCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
}

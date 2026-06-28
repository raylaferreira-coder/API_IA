package com.project.chat.dto.response;

public class DocumentChunkResponse {

    private Long chunkId;
    private Long documentId;
    private String fileName;
    private int chunkIndex;
    private String content;
    private Double similarityScore;

    public DocumentChunkResponse() {}

    public DocumentChunkResponse(Long chunkId, Long documentId, int chunkIndex,
                                  String content) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }
}

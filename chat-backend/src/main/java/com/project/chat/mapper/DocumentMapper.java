package com.project.chat.mapper;

import com.project.chat.dto.response.DocumentChunkResponse;
import com.project.chat.dto.response.DocumentResponse;
import com.project.chat.dto.response.IngestionResponse;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getSourceType(),
                document.getFileSize(),
                document.getStatus(),
                document.getTotalChunks(),
                document.getCreatedAt()
        );
    }

    public DocumentChunkResponse toChunkResponse(DocumentChunk chunk) {
        DocumentChunkResponse response = new DocumentChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getContent()
        );
        return response;
    }

    public IngestionResponse toIngestionResponse(Document document, String message) {
        return new IngestionResponse(
                document.getId(),
                document.getFileName(),
                document.getStatus(),
                document.getTotalChunks(),
                0L,
                message
        );
    }
}

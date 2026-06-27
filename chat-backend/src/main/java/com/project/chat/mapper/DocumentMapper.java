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
                document.getTitle(),
                document.getSourceUrl(),
                document.getSourceType(),
                document.getStatus(),
                document.getErrorMessage(),
                document.getChunkCount(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public DocumentChunkResponse toChunkResponse(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getTokenCount()
        );
    }

    public IngestionResponse toIngestionResponse(Document document, String message) {
        return new IngestionResponse(
                document.getId(),
                document.getTitle(),
                document.getStatus(),
                document.getChunkCount(),
                document.getCreatedAt(),
                message
        );
    }
}

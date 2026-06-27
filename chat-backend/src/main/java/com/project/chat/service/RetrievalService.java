package com.project.chat.service;

import com.project.chat.entity.DocumentChunk;
import com.project.chat.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    public RetrievalService(EmbeddingService embeddingService,
                            DocumentChunkRepository documentChunkRepository) {
        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<DocumentChunk> retrieveChunks(String query, int maxChunks) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorStr = toVectorString(queryEmbedding);
            return documentChunkRepository.findSimilarChunks(vectorStr, maxChunks);
        } catch (Exception e) {
            log.warn("Erro ao buscar chunks similares: {}", e.getMessage());
            return List.of();
        }
    }

    public String retrieveContext(String query, int maxChunks) {
        List<DocumentChunk> chunks = retrieveChunks(query, maxChunks);
        if (chunks.isEmpty()) {
            return "";
        }
        return chunks.stream()
                .map(c -> "- " + c.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}

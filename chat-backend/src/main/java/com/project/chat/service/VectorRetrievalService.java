package com.project.chat.service;

import com.project.chat.entity.DocumentChunk;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("rag")
public class VectorRetrievalService implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(VectorRetrievalService.class);

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;

    public VectorRetrievalService(DocumentChunkRepository documentChunkRepository,
                                  EmbeddingService embeddingService) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    public List<DocumentChunk> search(String query, int topK) {
        log.debug("Buscando chunks similares: query='{}', topK={}", query, topK);
        float[] queryEmbedding = embeddingService.embed(query);
        return search(queryEmbedding, topK);
    }

    @Override
    public List<DocumentChunk> search(float[] vector, int topK) {
        log.debug("Buscando chunks similares: vector=float[{}], topK={}", vector.length, topK);
        String vectorStr = FileUtils.toVectorString(vector);
        return documentChunkRepository.findSimilarChunks(vectorStr, topK);
    }
}

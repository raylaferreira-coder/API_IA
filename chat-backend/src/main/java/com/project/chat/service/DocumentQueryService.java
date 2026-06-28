package com.project.chat.service;

import com.project.chat.dto.response.DocumentChunkResponse;
import com.project.chat.dto.response.DocumentResponse;
import com.project.chat.dto.response.DocumentsListResponse;
import com.project.chat.dto.response.SearchResultResponse;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.mapper.DocumentMapper;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DocumentQueryService {

    private static final Logger log = LoggerFactory.getLogger(DocumentQueryService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentMapper documentMapper;
    private final EmbeddingService embeddingService;

    public DocumentQueryService(DocumentRepository documentRepository,
                                DocumentChunkRepository documentChunkRepository,
                                DocumentMapper documentMapper,
                                EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentMapper = documentMapper;
        this.embeddingService = embeddingService;
    }

    @Transactional(readOnly = true)
    public DocumentsListResponse listDocuments() {
        List<Document> documents = documentRepository.findAll();
        List<DocumentResponse> docs = documents.stream()
                .map(documentMapper::toDocumentResponse)
                .toList();
        return new DocumentsListResponse(docs);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + id));
        return documentMapper.toDocumentResponse(document);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado: " + documentId));
        documentChunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        log.info("Documento removido: id={}", documentId);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getDocumentChunks(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Documento não encontrado: " + documentId);
        }
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(documentId);
        return chunks.stream()
                .map(documentMapper::toChunkResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SearchResultResponse searchSimilar(String query, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorStr = toVectorString(queryEmbedding);
        List<DocumentChunk> chunks = documentChunkRepository.findSimilarChunks(vectorStr, topK);

        List<DocumentChunkResponse> results = chunks.stream()
                .map(chunk -> {
                    DocumentChunkResponse resp = documentMapper.toChunkResponse(chunk);
                    documentRepository.findById(chunk.getDocumentId())
                            .ifPresent(doc -> resp.setFileName(doc.getFileName()));
                    resp.setSimilarityScore(computeSimilarity(queryEmbedding, chunk.getEmbedding()));
                    return resp;
                })
                .toList();

        log.debug("Busca semântica: query='{}', topK={}, resultados={}", query, topK, results.size());
        return new SearchResultResponse(results);
    }

    private double computeSimilarity(float[] queryVec, float[] chunkVec) {
        if (queryVec == null || chunkVec == null || queryVec.length != chunkVec.length || queryVec.length == 0) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normQuery = 0.0;
        double normChunk = 0.0;
        for (int i = 0; i < queryVec.length; i++) {
            dotProduct += (double) queryVec[i] * chunkVec[i];
            normQuery += (double) queryVec[i] * queryVec[i];
            normChunk += (double) chunkVec[i] * chunkVec[i];
        }
        double denominator = Math.sqrt(normQuery) * Math.sqrt(normChunk);
        if (denominator == 0.0) {
            return 0.0;
        }
        return dotProduct / denominator;
    }

    private static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}

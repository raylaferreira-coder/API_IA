package com.project.chat.service;

import com.project.chat.entity.DocumentChunk;
import com.project.chat.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private EmbeddingService embeddingService;

    private VectorRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new VectorRetrievalService(documentChunkRepository, embeddingService);
    }

    @Test
    void search_ShouldReturnChunks() {
        String query = "Qual a origem do Thanos?";
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        DocumentChunk chunk1 = new DocumentChunk();
        DocumentChunk chunk2 = new DocumentChunk();
        List<DocumentChunk> expectedChunks = List.of(chunk1, chunk2);

        when(embeddingService.embed(query)).thenReturn(embedding);
        when(documentChunkRepository.findSimilarChunks(anyString(), eq(3))).thenReturn(expectedChunks);

        List<DocumentChunk> result = retrievalService.search(query, 3);

        assertEquals(2, result.size());
        verify(embeddingService).embed(query);
        verify(documentChunkRepository).findSimilarChunks(anyString(), eq(3));
    }

    @Test
    void search_WhenEmbeddingFails_ShouldThrowException() {
        String query = "consulta";

        when(embeddingService.embed(query)).thenThrow(new RuntimeException("Ollama indisponível"));

        assertThrows(RuntimeException.class, () -> retrievalService.search(query, 5));
        verifyNoInteractions(documentChunkRepository);
    }

    @Test
    void search_WithEmptyResult_ShouldReturnEmptyList() {
        String query = "consulta vazia";
        float[] embedding = new float[]{0.1f, 0.2f};

        when(embeddingService.embed(query)).thenReturn(embedding);
        when(documentChunkRepository.findSimilarChunks(anyString(), eq(10))).thenReturn(List.of());

        List<DocumentChunk> result = retrievalService.search(query, 10);

        assertTrue(result.isEmpty());
    }
}

package com.project.chat.service;

import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.exception.IngestionException;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.parser.ParserFactory;
import com.project.chat.service.parser.UrlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private ParserFactory parserFactory;
    @Mock
    private UrlParser urlParser;
    @Mock
    private ChunkService chunkService;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private WebhookService webhookService;

    private DocumentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionService(
                documentRepository, documentChunkRepository,
                parserFactory, urlParser,
                chunkService, embeddingService, webhookService);
    }

    @Test
    void ingestFromUrl_WithValidUrl_ShouldProcessSuccessfully() throws Exception {
        String url = "https://pt.wikipedia.org/wiki/MCU";
        String rawText = "Título: Universo Cinematográfico Marvel\nConteúdo sobre o MCU.";

        when(urlParser.parseUrl(url)).thenReturn(rawText);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });
        when(chunkService.chunkText(rawText)).thenReturn(List.of("Chunk 1", "Chunk 2"));
        when(chunkService.estimateTokens(anyString())).thenReturn(10);
        when(embeddingService.embedAll(anyList()))
                .thenReturn(List.of(new float[768], new float[768]));

        Document result = ingestionService.ingestFromUrl(url);

        assertNotNull(result);
        assertEquals(DocumentStatus.COMPLETED, result.getStatus());
        verify(urlParser).parseUrl(url);
        verify(documentRepository, atLeast(2)).save(any(Document.class));
        verify(documentChunkRepository).saveAll(anyList());
        verify(webhookService).notify(any(), anyString(), eq(DocumentStatus.COMPLETED), eq(2), anyLong());
    }

    @Test
    void ingestFromUrl_WhenUrlParserFails_ShouldThrowIngestionException() throws Exception {
        String url = "https://invalid-url.com";

        when(urlParser.parseUrl(url)).thenThrow(new RuntimeException("Connection refused"));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IngestionException.class, () -> ingestionService.ingestFromUrl(url));
        verify(documentRepository).save(any(Document.class));
        verify(webhookService).notify(any(), anyString(), eq(DocumentStatus.FAILED), eq(0), anyLong());
    }

    @Test
    void searchSimilar_ShouldReturnChunks() throws Exception {
        String query = "Qual a origem do Thanos?";
        float[] embedding = new float[768];
        List<DocumentChunk> expectedChunks = List.of(new DocumentChunk());

        when(embeddingService.embed(query)).thenReturn(embedding);
        when(documentChunkRepository.findSimilarChunks(anyString(), eq(5))).thenReturn(expectedChunks);

        List<DocumentChunk> result = ingestionService.searchSimilar(query, 5);

        assertEquals(expectedChunks, result);
        verify(embeddingService).embed(query);
        verify(documentChunkRepository).findSimilarChunks(anyString(), eq(5));
    }
}

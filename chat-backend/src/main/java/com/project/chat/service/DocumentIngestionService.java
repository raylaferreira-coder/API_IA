package com.project.chat.service;

import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.exception.IngestionException;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.parser.ParserFactory;
import com.project.chat.service.parser.UrlParser;
import com.project.chat.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ParserFactory parserFactory;
    private final UrlParser urlParser;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final WebhookService webhookService;

    public DocumentIngestionService(DocumentRepository documentRepository,
                                    DocumentChunkRepository documentChunkRepository,
                                    ParserFactory parserFactory,
                                    UrlParser urlParser,
                                    ChunkService chunkService,
                                    EmbeddingService embeddingService,
                                    WebhookService webhookService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.parserFactory = parserFactory;
        this.urlParser = urlParser;
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
        this.webhookService = webhookService;
    }

    @Transactional
    public Document ingestFromUrl(String url) {
        long startTime = System.currentTimeMillis();
        try {
            String rawText = urlParser.parseUrl(url);
            String title = extractTitle(rawText, url);

            Document document = new Document(title, url, "url");
            document.setStatus(DocumentStatus.PROCESSING);
            document = documentRepository.save(document);

            processDocument(document, rawText);

            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Documento processado com sucesso: id={}, url={}", document.getId(), url);

            webhookService.notify(document.getId(), url, DocumentStatus.COMPLETED,
                    document.getTotalChunks(), System.currentTimeMillis() - startTime);

            return document;

        } catch (Exception e) {
            Document document = new Document("Falha ao processar: " + url, url, "url");
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            document = documentRepository.save(document);
            log.error("Falha ao processar documento: id={}, url={}, error={}", document.getId(), url, e.getMessage());

            webhookService.notify(document.getId(), url, DocumentStatus.FAILED,
                    0, System.currentTimeMillis() - startTime);

            throw new IngestionException("Falha ao ingerir URL: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Document ingestFromFile(String filePath, String sourceType, String title) {
        long startTime = System.currentTimeMillis();
        Document document = new Document(
                title != null ? title : "Arquivo: " + filePath,
                filePath,
                sourceType
        );
        document.setStatus(DocumentStatus.PENDING);
        document = documentRepository.save(document);

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            try (InputStream inputStream = new FileInputStream(filePath)) {
                String rawText = parserFactory.getParser(sourceType).parse(inputStream);
                processDocument(document, rawText);
            }

            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Arquivo processado com sucesso: id={}, path={}", document.getId(), filePath);

            webhookService.notify(document.getId(), filePath, DocumentStatus.COMPLETED,
                    document.getTotalChunks(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            log.error("Falha ao processar arquivo: id={}, error={}", document.getId(), e.getMessage());

            webhookService.notify(document.getId(), filePath, DocumentStatus.FAILED,
                    0, System.currentTimeMillis() - startTime);

            throw new IngestionException("Falha ao ingerir arquivo: " + e.getMessage(), e);
        }

        return document;
    }

    private void processDocument(Document document, String rawText) {
        List<String> chunks = chunkService.chunkText(rawText);
        log.info("Gerando {} chunks para o documento {}", chunks.size(), document.getId());

        List<float[]> embeddings = embeddingService.embedAll(chunks);

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk(
                    document,
                    i,
                    chunks.get(i),
                    chunkService.estimateTokens(chunks.get(i))
            );
            if (i < embeddings.size()) {
                chunk.setEmbedding(embeddings.get(i));
            }
            documentChunkRepository.save(chunk);
        }

        document.setTotalChunks(chunks.size());
    }

    @Transactional
    public void updateDocument(Document document) {
        documentRepository.save(document);
    }

    public List<DocumentChunk> searchSimilar(String query, int limit) {
        float[] queryEmbedding = embeddingService.embed(query);
        return documentChunkRepository.findSimilarChunks(FileUtils.toVectorString(queryEmbedding), limit);
    }

    public List<DocumentChunk> searchSimilarByDocument(String query, Long documentId, int limit) {
        float[] queryEmbedding = embeddingService.embed(query);
        return documentChunkRepository.findSimilarChunksByDocument(
                documentId, FileUtils.toVectorString(queryEmbedding), limit);
    }

    private String extractTitle(String rawText, String url) {
        return rawText.lines()
                .filter(l -> l.startsWith("Título:"))
                .findFirst()
                .map(l -> l.replace("Título:", "").trim())
                .orElseGet(() -> {
                    String[] parts = url.split("/");
                    return parts[parts.length - 1].replaceAll("[-_]", " ");
                });
    }
}

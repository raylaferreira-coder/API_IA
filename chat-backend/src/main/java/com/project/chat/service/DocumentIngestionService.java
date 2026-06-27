package com.project.chat.service;

import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.exception.IngestionException;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ParserFactory parserFactory;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;

    public DocumentIngestionService(DocumentRepository documentRepository,
                                    DocumentChunkRepository documentChunkRepository,
                                    ParserFactory parserFactory,
                                    ChunkService chunkService,
                                    EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.parserFactory = parserFactory;
        this.chunkService = chunkService;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public Document ingestFromUrl(String url) {
        Document document = new Document(
                extrairTituloDaUrl(url),
                url,
                "url"
        );
        document.setStatus(DocumentStatus.PENDING);
        document = documentRepository.save(document);

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            String rawText = parserFactory.getParser("url").parse(url);
            processDocument(document, rawText);

            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Documento processado com sucesso: id={}, url={}", document.getId(), url);

        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            log.error("Falha ao processar documento: id={}, error={}", document.getId(), e.getMessage());
            throw new IngestionException("Falha ao ingerir URL: " + e.getMessage(), e);
        }

        return document;
    }

    @Transactional
    public Document ingestFromFile(String filePath, String sourceType, String title) {
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

            String rawText = parserFactory.getParser(sourceType).parse(filePath);
            processDocument(document, rawText);

            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Arquivo processado com sucesso: id={}, path={}", document.getId(), filePath);

        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            log.error("Falha ao processar arquivo: id={}, error={}", document.getId(), e.getMessage());
            throw new IngestionException("Falha ao ingerir arquivo: " + e.getMessage(), e);
        }

        return document;
    }

    private void processDocument(Document document, String rawText) {
        List<String> chunks = chunkService.chunkText(rawText);
        log.info("Gerando {} chunks para o documento {}", chunks.size(), document.getId());

        List<float[]> embeddings = embeddingService.generateEmbeddings(chunks);

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk(
                    document.getId(),
                    i,
                    chunks.get(i),
                    chunkService.estimateTokens(chunks.get(i))
            );
            if (i < embeddings.size()) {
                chunk.setEmbedding(embeddings.get(i));
            }
            documentChunkRepository.save(chunk);
        }

        document.setChunkCount(chunks.size());
    }

    public List<DocumentChunk> searchSimilar(String query, int limit) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        return documentChunkRepository.findSimilarChunks(toVectorString(queryEmbedding), limit);
    }

    public List<DocumentChunk> searchSimilarByDocument(String query, Long documentId, int limit) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        return documentChunkRepository.findSimilarChunksByDocument(
                documentId, toVectorString(queryEmbedding), limit);
    }

    public static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String extrairTituloDaUrl(String url) {
        try {
            return parserFactory.getParser("url").parse(url).lines()
                    .filter(l -> l.startsWith("Título:"))
                    .findFirst()
                    .map(l -> l.replace("Título:", "").trim())
                    .orElseGet(() -> {
                        String[] partes = url.split("/");
                        return partes[partes.length - 1].replaceAll("[-_]", " ");
                    });
        } catch (Exception e) {
            String[] partes = url.split("/");
            return partes[partes.length - 1].replaceAll("[-_]", " ");
        }
    }
}

package com.project.chat.service.marvel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.ChunkService;
import com.project.chat.service.EmbeddingService;
import com.project.chat.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MarvelWikipediaIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarvelWikipediaIngestionService.class);

    private static final String WIKIPEDIA_API = "https://en.wikipedia.org/w/api.php";
    private static final List<String> MARVEL_PAGES = List.of(
            "Marvel_Cinematic_Universe",
            "Marvel_Comics",
            "Avengers_(Marvel_Cinematic_Universe)",
            "Iron_Man_(Marvel_Cinematic_Universe)",
            "Captain_America_(Marvel_Cinematic_Universe)",
            "Thor_(Marvel_Cinematic_Universe)",
            "Hulk_(Marvel_Cinematic_Universe)",
            "Black_Widow_(Marvel_Cinematic_Universe)",
            "Spider-Man_(Marvel_Cinematic_Universe)",
            "Guardians_of_the_Galaxy_(Marvel_Cinematic_Universe)",
            "Doctor_Strange_(Marvel_Cinematic_Universe)",
            "Black_Panther_(Marvel_Cinematic_Universe)",
            "Captain_Marvel_(Marvel_Cinematic_Universe)",
            "Ant-Man_(Marvel_Cinematic_Universe)",
            "WandaVision",
            "The_Mandalorian"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final ChunkService chunkService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final WebhookService webhookService;

    public MarvelWikipediaIngestionService(EmbeddingService embeddingService,
                                           ChunkService chunkService,
                                           DocumentRepository documentRepository,
                                           DocumentChunkRepository documentChunkRepository,
                                           WebhookService webhookService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.webhookService = webhookService;
    }

    public WikipediaIngestionResult ingestAll() {
        long startTime = System.currentTimeMillis();
        int totalDocuments = 0;
        int totalChunks = 0;
        int errors = 0;

        log.info("Iniciando ingestao Wikipedia Marvel com {} paginas...", MARVEL_PAGES.size());

        for (String pageTitle : MARVEL_PAGES) {
            try {
                Document doc = ingestSinglePage(pageTitle);
                if (doc != null) {
                    totalDocuments++;
                    totalChunks += doc.getTotalChunks();
                }
            } catch (Exception e) {
                errors++;
                log.warn("Erro ao ingerir pagina Wikipedia '{}': {}", pageTitle, e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Ingestao Wikipedia concluida: {} documentos, {} chunks, {} erros, {}ms",
                totalDocuments, totalChunks, errors, elapsed);

        return new WikipediaIngestionResult(totalDocuments, totalChunks, errors, elapsed);
    }

    @Transactional
    public Document ingestSinglePage(String pageTitle) {
        long startTime = System.currentTimeMillis();

        try {
            String content = fetchWikipediaContent(pageTitle);
            if (content == null || content.isBlank()) {
                log.warn("Conteudo vazio para Wikipedia page: {}", pageTitle);
                return null;
            }

            String pageUrl = "https://en.wikipedia.org/wiki/" +
                    URLEncoder.encode(pageTitle.replace(" ", "_"), StandardCharsets.UTF_8);

            Document document = new Document(pageTitle.replace("_", " "), pageUrl, "marvel-wikipedia");
            document.setStatus(DocumentStatus.PROCESSING);
            document = documentRepository.save(document);

            List<String> chunks = chunkService.chunkText(content);
            List<float[]> embeddings = embeddingService.embedAll(chunks);

            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk(document, i, chunks.get(i),
                        chunkService.estimateTokens(chunks.get(i)));
                if (i < embeddings.size()) {
                    chunk.setEmbedding(embeddings.get(i));
                }
                chunkEntities.add(chunk);
            }
            documentChunkRepository.saveAll(chunkEntities);

            document.setTotalChunks(chunks.size());
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);

            long elapsed = System.currentTimeMillis() - startTime;
            webhookService.notify(document.getId(), pageTitle, DocumentStatus.COMPLETED,
                    chunks.size(), elapsed);

            log.info("Pagina Wikipedia ingerida: {} ({} chunks, {}ms)", pageTitle, chunks.size(), elapsed);
            return document;

        } catch (Exception e) {
            log.error("Erro ao ingerir pagina Wikipedia {}: {}", pageTitle, e.getMessage());
            return null;
        }
    }

    private String fetchWikipediaContent(String pageTitle) throws Exception {
        String url = String.format(
                "%s?action=query&prop=extracts&exlimit=1&explaintext=1&format=json&titles=%s",
                WIKIPEDIA_API,
                URLEncoder.encode(pageTitle, StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Marvel-RAG-Bot/1.0 (rag@marvelchat.local)")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Wikipedia API retornou {} para {}", response.statusCode(), pageTitle);
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode pages = root.path("query").path("pages");
        if (pages.isMissingNode() || pages.isEmpty()) return null;

        String extract = pages.elements().next().path("extract").asText("");
        return extract.isBlank() ? null : extract;
    }

    public static class WikipediaIngestionResult {
        private final int documents;
        private final int chunks;
        private final int errors;
        private final long elapsed;

        public WikipediaIngestionResult(int documents, int chunks, int errors, long elapsed) {
            this.documents = documents;
            this.chunks = chunks;
            this.errors = errors;
            this.elapsed = elapsed;
        }

        public int getDocuments() { return documents; }
        public int getChunks() { return chunks; }
        public int getErrors() { return errors; }
        public long getElapsed() { return elapsed; }
    }
}

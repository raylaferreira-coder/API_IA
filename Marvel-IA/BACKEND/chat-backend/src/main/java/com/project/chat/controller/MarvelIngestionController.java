package com.project.chat.controller;

import com.project.chat.dto.response.MarvelIngestionResponse;
import com.project.chat.dto.response.MarvelIngestionResponse.MarvelSourceResult;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.ChunkService;
import com.project.chat.service.EmbeddingService;
import com.project.chat.service.WebhookService;
import com.project.chat.service.marvel.MarvelApiService;
import com.project.chat.service.marvel.MarvelFandomIngestionService;
import com.project.chat.service.marvel.MarvelFandomIngestionService.FandomIngestionResult;
import com.project.chat.service.marvel.MarvelWikipediaIngestionService;
import com.project.chat.service.marvel.MarvelWikipediaIngestionService.WikipediaIngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marvel")
public class MarvelIngestionController {

    private static final Logger log = LoggerFactory.getLogger(MarvelIngestionController.class);

    private final MarvelFandomIngestionService fandomIngestionService;
    private final MarvelWikipediaIngestionService wikipediaIngestionService;
    private final MarvelApiService marvelApiService;
    private final EmbeddingService embeddingService;
    private final ChunkService chunkService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final WebhookService webhookService;

    public MarvelIngestionController(MarvelFandomIngestionService fandomIngestionService,
                                     MarvelWikipediaIngestionService wikipediaIngestionService,
                                     MarvelApiService marvelApiService,
                                     EmbeddingService embeddingService,
                                     ChunkService chunkService,
                                     DocumentRepository documentRepository,
                                     DocumentChunkRepository documentChunkRepository,
                                     WebhookService webhookService) {
        this.fandomIngestionService = fandomIngestionService;
        this.wikipediaIngestionService = wikipediaIngestionService;
        this.marvelApiService = marvelApiService;
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.webhookService = webhookService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "marvelApiConfigured", marvelApiService.isConfigured(),
                "fandomSource", "marvel.fandom.com",
                "wikipediaSource", "en.wikipedia.org",
                "marvelApiSource", "gateway.marvel.com/v1/public"
        ));
    }

    @PostMapping("/ingest/fandom")
    public ResponseEntity<MarvelIngestionResponse> ingestFandom() {
        FandomIngestionResult result = fandomIngestionService.ingestAll();
        MarvelIngestionResponse response = new MarvelIngestionResponse(
                "marvel-fandom", true,
                result.getDocuments(), result.getChunks(),
                result.getErrors(), result.getElapsed()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ingest/fandom/page")
    public ResponseEntity<MarvelIngestionResponse> ingestFandomPage(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Document doc = fandomIngestionService.ingestSinglePage(url);
        if (doc == null) {
            return ResponseEntity.ok(new MarvelIngestionResponse("marvel-fandom", true, 0, 0, 1, 0));
        }
        MarvelIngestionResponse response = new MarvelIngestionResponse(
                "marvel-fandom", true, 1, doc.getTotalChunks(), 0, 0
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ingest/wikipedia")
    public ResponseEntity<MarvelIngestionResponse> ingestWikipedia() {
        WikipediaIngestionResult result = wikipediaIngestionService.ingestAll();
        MarvelIngestionResponse response = new MarvelIngestionResponse(
                "marvel-wikipedia", true,
                result.getDocuments(), result.getChunks(),
                result.getErrors(), result.getElapsed()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ingest/wikipedia/page")
    public ResponseEntity<MarvelIngestionResponse> ingestWikipediaPage(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Document doc = wikipediaIngestionService.ingestSinglePage(title);
        if (doc == null) {
            return ResponseEntity.ok(new MarvelIngestionResponse("marvel-wikipedia", true, 0, 0, 1, 0));
        }
        MarvelIngestionResponse response = new MarvelIngestionResponse(
                "marvel-wikipedia", true, 1, doc.getTotalChunks(), 0, 0
        );
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping("/ingest/marvel-api")
    public ResponseEntity<MarvelIngestionResponse> ingestFromMarvelApi() {
        if (!marvelApiService.isConfigured()) {
            return ResponseEntity.ok(new MarvelIngestionResponse("marvel-api", false, 0, 0, 0, 0));
        }

        long startTime = System.currentTimeMillis();
        int totalChunks = 0;
        int errors = 0;

        List<MarvelApiService.MarvelCharacter> characters = marvelApiService.fetchCharacters(0);
        StringBuilder content = new StringBuilder();
        for (var c : characters) {
            if (c.getDescription() != null && !c.getDescription().isBlank()) {
                content.append("Personagem: ").append(c.getName()).append("\n");
                content.append("Descricao: ").append(c.getDescription()).append("\n");
                if (!c.getComics().isEmpty()) {
                    content.append("Aparicoes em comics: ").append(String.join(", ", c.getComics())).append("\n");
                }
                content.append("\n---\n\n");
            }
        }

        if (content.length() > 0) {
            Document doc = new Document("Marvel API - Characters", "marvel-api:characters", "marvel-api");
            doc.setStatus(DocumentStatus.PROCESSING);
            doc = documentRepository.save(doc);

            List<String> chunks = chunkService.chunkText(content.toString());
            List<float[]> embeddings = embeddingService.embedAll(chunks);

            List<DocumentChunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk chunk = new DocumentChunk(doc, i, chunks.get(i),
                        chunkService.estimateTokens(chunks.get(i)));
                if (i < embeddings.size()) {
                    chunk.setEmbedding(embeddings.get(i));
                }
                chunkEntities.add(chunk);
            }
            documentChunkRepository.saveAll(chunkEntities);

            doc.setTotalChunks(chunks.size());
            doc.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(doc);
            totalChunks = chunks.size();

            webhookService.notify(doc.getId(), "Marvel API Characters", DocumentStatus.COMPLETED,
                    chunks.size(), System.currentTimeMillis() - startTime);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(new MarvelIngestionResponse(
                "marvel-api", true, characters.size(), totalChunks, errors, elapsed
        ));
    }

    @PostMapping("/ingest/all")
    public ResponseEntity<MarvelIngestionResponse> ingestAll() {
        long startTime = System.currentTimeMillis();
        int totalDocs = 0;
        int totalChunks = 0;
        int totalErrors = 0;

        List<MarvelSourceResult> sources = new ArrayList<>();

        fandom:
        {
            FandomIngestionResult r = fandomIngestionService.ingestAll();
            sources.add(new MarvelSourceResult("marvel-fandom", r.getDocuments(), r.getChunks(), r.getErrors(), r.getElapsed()));
            totalDocs += r.getDocuments();
            totalChunks += r.getChunks();
            totalErrors += r.getErrors();
        }

        wiki:
        {
            WikipediaIngestionResult r = wikipediaIngestionService.ingestAll();
            sources.add(new MarvelSourceResult("marvel-wikipedia", r.getDocuments(), r.getChunks(), r.getErrors(), r.getElapsed()));
            totalDocs += r.getDocuments();
            totalChunks += r.getChunks();
            totalErrors += r.getErrors();
        }

        if (marvelApiService.isConfigured()) {
            long apiStart = System.currentTimeMillis();
            var chars = marvelApiService.fetchCharacters(0);
            long apiElapsed = System.currentTimeMillis() - apiStart;
            sources.add(new MarvelSourceResult("marvel-api", chars.size(), 0, 0, apiElapsed));
            totalDocs += chars.size();
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        MarvelIngestionResponse response = new MarvelIngestionResponse("all-sources", true,
                totalDocs, totalChunks, totalErrors, totalElapsed);
        response.setSources(sources);
        return ResponseEntity.ok(response);
    }
}

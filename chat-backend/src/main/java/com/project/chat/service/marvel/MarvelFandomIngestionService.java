package com.project.chat.service.marvel;

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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MarvelFandomIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarvelFandomIngestionService.class);

    private static final String FANDOM_BASE = "https://marvel.fandom.com";
    private static final List<String> CHARACTER_PAGES = List.of(
            "/wiki/Category:Characters",
            "/wiki/Category:Avengers_members",
            "/wiki/Category:X-Men_members",
            "/wiki/Category:Guardians_of_the_Galaxy_members",
            "/wiki/Category:Villains"
    );

    private final HttpClient httpClient;
    private final EmbeddingService embeddingService;
    private final ChunkService chunkService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final WebhookService webhookService;

    public MarvelFandomIngestionService(EmbeddingService embeddingService,
                                        ChunkService chunkService,
                                        DocumentRepository documentRepository,
                                        DocumentChunkRepository documentChunkRepository,
                                        WebhookService webhookService) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.webhookService = webhookService;
    }

    public FandomIngestionResult ingestAll() {
        long startTime = System.currentTimeMillis();
        int totalDocuments = 0;
        int totalChunks = 0;
        int errors = 0;

        log.info("Iniciando ingestao Fandom Marvel...");

        for (String categoryUrl : CHARACTER_PAGES) {
            try {
                List<String> pageUrls = fetchPageUrlsFromCategory(categoryUrl);
                log.info("Categoria {}: {} paginas encontradas", categoryUrl, pageUrls.size());

                for (String pageUrl : pageUrls) {
                    try {
                        Document doc = ingestSinglePage(pageUrl);
                        if (doc != null) {
                            totalDocuments++;
                            totalChunks += doc.getTotalChunks();
                        }
                    } catch (Exception e) {
                        errors++;
                        log.warn("Erro ao ingerir pagina {}: {}", pageUrl, e.getMessage());
                    }
                }
            } catch (Exception e) {
                errors++;
                log.warn("Erro ao processar categoria {}: {}", categoryUrl, e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Ingestao Fandom concluida: {} documentos, {} chunks, {} erros, {}ms",
                totalDocuments, totalChunks, errors, elapsed);

        return new FandomIngestionResult(totalDocuments, totalChunks, errors, elapsed);
    }

    @Transactional
    public Document ingestSinglePage(String pageUrl) {
        long startTime = System.currentTimeMillis();

        try {
            String fullUrl = pageUrl.startsWith("http") ? pageUrl : FANDOM_BASE + pageUrl;
            String html = fetchUrl(fullUrl);
            String title = extractTitle(html);
            String content = extractContent(html);

            if (content == null || content.isBlank()) {
                log.warn("Conteudo vazio para {}", fullUrl);
                return null;
            }

            Document document = new Document(title, fullUrl, "marvel-fandom");
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
            webhookService.notify(document.getId(), title, DocumentStatus.COMPLETED,
                    chunks.size(), elapsed);

            log.info("Pagina Fandom ingerida: {} ({} chunks, {}ms)", title, chunks.size(), elapsed);
            return document;

        } catch (Exception e) {
            log.error("Erro ao ingerir pagina Fandom {}: {}", pageUrl, e.getMessage());
            return null;
        }
    }

    private List<String> fetchPageUrlsFromCategory(String categoryPath) {
        List<String> urls = new ArrayList<>();
        try {
            String fullUrl = FANDOM_BASE + categoryPath;
            String html = fetchUrl(fullUrl);
            int idx = 0;
            while (true) {
                int linkStart = html.indexOf("<a href=\"/wiki/", idx);
                if (linkStart < 0) break;
                int linkEnd = html.indexOf("\"", linkStart + 9);
                if (linkEnd < 0) break;
                String href = html.substring(linkStart + 9, linkEnd);
                if (!href.contains(":") && !href.contains("Category") && !href.contains("Special")) {
                    String fullPageUrl = FANDOM_BASE + href;
                    if (!urls.contains(fullPageUrl)) {
                        urls.add(fullPageUrl);
                    }
                }
                idx = linkEnd + 1;
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar URLs da categoria {}: {}", categoryPath, e.getMessage());
        }
        return urls;
    }

    private String fetchUrl(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Marvel-RAG-Bot/1.0")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String extractTitle(String html) {
        int start = html.indexOf("<title>");
        if (start < 0) return "Unknown";
        start += 7;
        int end = html.indexOf("</title>", start);
        if (end < 0) return "Unknown";
        String title = html.substring(start, end);
        int pipe = title.indexOf(" |");
        if (pipe > 0) title = title.substring(0, pipe);
        return title.trim();
    }

    private String extractContent(String html) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;

        boolean inTag = false;
        boolean inScript = false;
        boolean inStyle = false;

        while (idx < html.length()) {
            char c = html.charAt(idx);

            if (c == '<') {
                inTag = true;
                String tag = getTagName(html, idx);
                if ("script".equalsIgnoreCase(tag)) inScript = true;
                if ("style".equalsIgnoreCase(tag)) inStyle = true;
            }

            if (!inTag && !inScript && !inStyle) {
                sb.append(c);
            }

            if (c == '>') {
                inTag = false;
                if (inScript && html.substring(idx - 8 > 0 ? idx - 8 : 0, idx + 1).contains("</script>")) {
                    inScript = false;
                }
                if (inStyle && html.substring(idx - 7 > 0 ? idx - 7 : 0, idx + 1).contains("</style>")) {
                    inStyle = false;
                }
            }

            idx++;
        }

        String text = sb.toString()
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return text.length() > 50 ? text : null;
    }

    private String getTagName(String html, int start) {
        int end = start + 1;
        while (end < html.length() && Character.isLetterOrDigit(html.charAt(end))) {
            end++;
        }
        if (html.charAt(start + 1) == '/') {
            return html.substring(start + 2, end);
        }
        return html.substring(start + 1, end);
    }

    public static class FandomIngestionResult {
        private final int documents;
        private final int chunks;
        private final int errors;
        private final long elapsed;

        public FandomIngestionResult(int documents, int chunks, int errors, long elapsed) {
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

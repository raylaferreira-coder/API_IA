package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    private final ExecutorService executor;

    public OllamaEmbeddingService(
            @Value("${rag.ollama.url:http://localhost:11434}") String baseUrl,
            @Value("${rag.ollama.embedding-model:nomic-embed-text}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newFixedThreadPool(5);
        log.info("Ollama Embedding Service iniciado: url={}, model={}", baseUrl, model);
    }

    @Override
    public float[] embed(String text) {
        try {
            String input = text.length() > 8000 ? text.substring(0, 8000) : text;

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", input
            );

            String json = objectMapper.writeValueAsString(requestBody);
            String normalizedUrl = baseUrl.replaceAll("/+$", "") + "/api/embed";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new EmbeddingException("Falha ao gerar embedding: HTTP " + response.statusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            if (responseMap == null || !responseMap.containsKey("embedding")) {
                throw new EmbeddingException("Resposta do Ollama não contém embedding.");
            }
            @SuppressWarnings("unchecked")
            List<Number> embeddingList = (List<Number>) responseMap.get("embedding");
            if (embeddingList == null) {
                throw new EmbeddingException("Lista de embeddings retornada como nula.");
            }

            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            return embedding;

        } catch (Exception e) {
            log.error("Erro ao gerar embedding: {}", e.getMessage());
            throw new EmbeddingException("Erro ao gerar embedding: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<CompletableFuture<float[]>> futures = texts.stream()
                .map(text -> CompletableFuture.supplyAsync(() -> embed(text), executor)
                        .orTimeout(30, TimeUnit.SECONDS))
                .toList();
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
}

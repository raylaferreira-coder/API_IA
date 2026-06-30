package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.exception.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("rag")
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private static final int MAX_CHARS = 8000;
    private static final int MAX_PARALLEL = 2;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    private final java.time.Duration readTimeout;

    public OllamaEmbeddingService(
            @Value("${rag.ollama.url:http://localhost:11434}") String baseUrl,
            @Value("${rag.ollama.embedding-model:nomic-embed-text}") String model,
            @Value("${rag.ollama.read-timeout:120s}") java.time.Duration readTimeout,
            HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.readTimeout = readTimeout;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        log.info("Ollama Embedding Service iniciado: url={}, model={}, readTimeout={}s", baseUrl, model, readTimeout.toSeconds());
    }

    @Override
    public float[] embed(String text) {
        try {
            String input = text;
            if (text.length() > MAX_CHARS) {
                input = text.substring(0, MAX_CHARS);
                log.warn("Texto truncado de {} para {} caracteres para embedding", text.length(), MAX_CHARS);
            }

            String normalizedUrl = baseUrl.replaceAll("/+$", "") + "/api/embed";

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "input", input
            );

            String json = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .header("Content-Type", "application/json")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404 && normalizedUrl.contains("/api/embed")) {
                String legacyUrl = baseUrl.replaceAll("/+$", "") + "/api/embeddings";
                log.warn("Endpoint /api/embed nao encontrado. Tentando /api/embeddings (compatibilidade)");
                requestBody = Map.of(
                        "model", model,
                        "prompt", input
                );
                json = objectMapper.writeValueAsString(requestBody);
                request = HttpRequest.newBuilder()
                        .uri(URI.create(legacyUrl))
                        .header("Content-Type", "application/json")
                        .timeout(readTimeout)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() != 200) {
                throw new EmbeddingException("Falha ao gerar embedding: HTTP " + response.statusCode() + " - " + response.body());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            if (responseMap == null) {
                throw new EmbeddingException("Resposta do Ollama vazia.");
            }

            List<List<Number>> embeddingsList = extractEmbeddings(responseMap);
            if (embeddingsList == null || embeddingsList.isEmpty()) {
                throw new EmbeddingException("Lista de embeddings retornada como vazia.");
            }

            List<Number> embeddingList = embeddingsList.get(0);
            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }

            return embedding;

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao gerar embedding: {}", e.getMessage());
            throw new EmbeddingException("Erro ao gerar embedding: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<List<Number>> extractEmbeddings(Map<String, Object> responseMap) {
        Object raw = responseMap.get("embeddings");
        if (raw instanceof List) {
            return (List<List<Number>>) raw;
        }
        raw = responseMap.get("embedding");
        if (raw instanceof List) {
            List<List<Number>> result = new ArrayList<>();
            result.add((List<Number>) raw);
            return result;
        }
        return null;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += MAX_PARALLEL) {
            int end = Math.min(i + MAX_PARALLEL, texts.size());
            List<String> batch = texts.subList(i, end);
            results.addAll(batch.parallelStream()
                    .map(this::embed)
                    .toList());
        }
        return results;
    }
}

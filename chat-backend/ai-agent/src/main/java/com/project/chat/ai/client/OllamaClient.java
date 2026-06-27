package com.project.chat.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.chat.ai.config.AiAgentProperties;
import com.project.chat.ai.dto.EmbeddingRequest;
import com.project.chat.ai.dto.EmbeddingResponse;
import com.project.chat.ai.dto.GenerateRequest;
import com.project.chat.ai.dto.GenerateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final AiAgentProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaClient(AiAgentProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public GenerateResponse generate(GenerateRequest request) {
        try {
            String url = properties.getBaseUrl() + "/api/generate";
            String jsonRequest = objectMapper.writeValueAsString(request);

            log.debug("Enviando request para Ollama generate: model={}, url={}",
                    request.getModel(), url);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(properties.getReadTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("Ollama retornou erro {}: {}", httpResponse.statusCode(), httpResponse.body());
                throw new RuntimeException("Ollama retornou status " + httpResponse.statusCode());
            }

            GenerateResponse response = objectMapper.readValue(
                    httpResponse.body(), GenerateResponse.class);

            log.debug("Resposta recebida do Ollama: done={}, chars={}",
                    response.isDone(), response.getResponse() != null ? response.getResponse().length() : 0);

            return response;

        } catch (Exception e) {
            log.error("Falha na comunicacao com Ollama: {}", e.getMessage());
            throw new RuntimeException("Erro ao comunicar com Ollama: " + e.getMessage(), e);
        }
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        try {
            String url = properties.getBaseUrl() + "/api/embeddings";
            String jsonRequest = objectMapper.writeValueAsString(request);

            log.debug("Enviando request para Ollama embeddings: model={}", request.getModel());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(properties.getReadTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("Ollama embeddings retornou erro {}: {}", httpResponse.statusCode(), httpResponse.body());
                throw new RuntimeException("Ollama embeddings retornou status " + httpResponse.statusCode());
            }

            return objectMapper.readValue(httpResponse.body(), EmbeddingResponse.class);

        } catch (Exception e) {
            log.error("Falha no embedding com Ollama: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar embedding via Ollama: " + e.getMessage(), e);
        }
    }

    public boolean healthCheck() {
        try {
            String url = properties.getBaseUrl();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Health check do Ollama falhou: {}", e.getMessage());
            return false;
        }
    }
}

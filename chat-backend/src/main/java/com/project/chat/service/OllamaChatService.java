package com.project.chat.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.exception.LlmServiceException;

@Service
@Profile("rag")
public class OllamaChatService {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final Duration readTimeout;
    private final ObjectMapper objectMapper;

    public OllamaChatService(HttpClient httpClient,
            @Value("${rag.ollama.url:http://localhost:11434}") String baseUrl,
            @Value("${rag.ollama.model:gemma3:4b}") String model,
            @Value("${rag.ollama.temperature:0.7}") double temperature,
            @Value("${rag.ollama.max-tokens:2048}") int maxTokens,
            @Value("${rag.ollama.read-timeout:120s}") Duration readTimeout) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.readTimeout = readTimeout;
        this.objectMapper = new ObjectMapper();
    }

    public String generate(String prompt) {
        try {
            String url = baseUrl + "/api/generate";
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of(
                            "temperature", temperature,
                            "num_predict", maxTokens));
            String jsonRequest = objectMapper.writeValueAsString(requestBody);

            log.debug("Enviando request para Ollama generate: model={}", model);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama retornou erro {}: {}", response.statusCode(), response.body());
                throw new LlmServiceException("Ollama retornou status " + response.statusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            String result = (String) responseBody.get("response");
            if (result == null || result.isBlank()) {
                log.error("Ollama retornou resposta vazia: {}", response.body());
                throw new LlmServiceException("Ollama retornou resposta vazia.");
            }

            log.debug("Resposta recebida do Ollama: chars={}", result.length());

            return result;

        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na comunicacao com Ollama: {}", e.getMessage());
            throw new LlmServiceException("Erro ao comunicar com Ollama: " + e.getMessage(), e);
        }
    }

    public void generateStream(String prompt, Consumer<String> onToken, Runnable onDone, Consumer<Throwable> onError) {
        try {
            String url = baseUrl + "/api/generate";
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", true,
                    "options", Map.of(
                            "temperature", temperature,
                            "num_predict", maxTokens));
            String jsonRequest = objectMapper.writeValueAsString(requestBody);

            log.debug("Enviando request streaming para Ollama: model={}", model);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes());
                log.error("Ollama streaming retornou erro {}: {}", response.statusCode(), errorBody);
                onError.accept(new LlmServiceException("Ollama retornou status " + response.statusCode()));
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty())
                        continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> chunk = objectMapper.readValue(line, Map.class);
                    String token = (String) chunk.get("response");
                    boolean done = Boolean.TRUE.equals(chunk.get("done"));

                    if (token != null && !token.isEmpty()) {
                        onToken.accept(token);
                    }

                    if (done) {
                        onDone.run();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Falha no streaming com Ollama: {}", e.getMessage());
            onError.accept(new LlmServiceException("Erro no streaming com Ollama: " + e.getMessage(), e));
        }
    }
}

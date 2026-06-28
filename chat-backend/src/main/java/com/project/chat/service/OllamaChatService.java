package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.exception.LlmServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@Profile("rag")
public class OllamaChatService {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final ObjectMapper objectMapper;

    public OllamaChatService(HttpClient httpClient,
                              @Value("${rag.ollama.url:http://localhost:11434}") String baseUrl,
                              @Value("${rag.ollama.model:llama3.2}") String model,
                              @Value("${rag.ollama.temperature:0.7}") double temperature,
                              @Value("${rag.ollama.max-tokens:2048}") int maxTokens) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
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
                            "num_predict", maxTokens
                    )
            );
            String jsonRequest = objectMapper.writeValueAsString(requestBody);

            log.debug("Enviando request para Ollama generate: model={}", model);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
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

            log.debug("Resposta recebida do Ollama: chars={}",
                    result != null ? result.length() : 0);

            return result;

        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha na comunicacao com Ollama: {}", e.getMessage());
            throw new LlmServiceException("Erro ao comunicar com Ollama: " + e.getMessage(), e);
        }
    }
}

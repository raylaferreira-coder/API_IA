package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.config.RagWebhookProperties;
import com.project.chat.entity.DocumentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RagWebhookProperties properties;

    public WebhookService(RagWebhookProperties properties) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.properties = properties;
    }

    public void notify(Long documentId, String fileName, DocumentStatus status, int chunks, long processingTime) {
        if (!properties.isEnabled() || properties.getUrl() == null || properties.getUrl().isBlank()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "documentId", documentId,
                "fileName", fileName != null ? fileName : "",
                "status", status != null ? status.name() : "UNKNOWN",
                "chunks", chunks,
                "embeddingModel", "nomic-embed-text",
                "processingTime", processingTime,
                "timestamp", LocalDateTime.now().toString()
        );

        int maxAttempts = Math.max(1, properties.getRetryAttempts());
        int delayMs = Math.max(0, properties.getRetryDelay());
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String json = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getUrl()))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                log.info("Webhook n8n enviado: status={}, documentId={}, tentativa={}/{}",
                        response.statusCode(), documentId, attempt, maxAttempts);
                return;

            } catch (Exception e) {
                lastException = e;
                log.warn("Falha ao enviar webhook n8n (documentId={}, tentativa={}/{}): {}",
                        documentId, attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Webhook n8n falhou após {} tentativas (documentId={})", maxAttempts, documentId, lastException);
    }
}

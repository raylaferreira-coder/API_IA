package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.entity.DocumentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final String webhookUrl;
    private final boolean enabled;

    public WebhookService(
            @Value("${n8n.webhook.url:}") String webhookUrl,
            @Value("${n8n.webhook.enabled:false}") boolean enabled) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.webhookUrl = webhookUrl;
        this.enabled = enabled;
    }

    public void notify(Long documentId, String fileName, DocumentStatus status, int chunks, long processingTime) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "documentId", documentId,
                    "fileName", fileName != null ? fileName : "",
                    "status", status != null ? status.name() : "UNKNOWN",
                    "chunks", chunks,
                    "embeddingModel", "nomic-embed-text",
                    "processingTime", processingTime,
                    "timestamp", LocalDateTime.now().toString()
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Webhook n8n enviado: status={}, documentId={}", response.statusCode(), documentId);

        } catch (Exception e) {
            log.warn("Falha ao enviar webhook n8n (documentId={}): {}", documentId, e.getMessage());
        }
    }

}

package com.project.chat.controller;

import com.project.chat.dto.response.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final HttpClient httpClient;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${chat.version:1.0.0}")
    private String version;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
        this.httpClient = HttpClient.newHttpClient();
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        String dbStatus = "UP";
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(2)) {
                dbStatus = "DOWN";
            }
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        String ollamaStatus = "UP";
        try {
            String normalizedUrl = ollamaBaseUrl.replaceAll("/+$", "") + "/api/tags";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedUrl))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                ollamaStatus = "DOWN";
            }
        } catch (Exception e) {
            ollamaStatus = "DOWN";
            log.warn("Ollama health check falhou: {}", e.getMessage());
        }

        File root = new File(".");
        long freeBytes = root.getFreeSpace();
        String diskInfo = "OK (" + (freeBytes / (1024 * 1024 * 1024)) + " GB disponível)";

        boolean dbUp = dbStatus.equals("UP");
        boolean ollamaUp = ollamaStatus.equals("UP");
        String overallStatus = dbUp && ollamaUp ? "UP" : dbUp ? "DEGRADED" : "DOWN";

        HealthResponse response = new HealthResponse(
                overallStatus,
                dbStatus,
                ollamaStatus,
                diskInfo,
                LocalDateTime.now(),
                version
        );

        return ResponseEntity.ok(response);
    }
}

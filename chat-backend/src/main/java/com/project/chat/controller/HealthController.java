package com.project.chat.controller;

import com.project.chat.dto.response.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    @Value("${spring.application.name:chat-backend}")
    private String appName;

    @Value("${chat.version:1.0.0}")
    private String version;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
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

        File root = new File(".");
        long freeBytes = root.getFreeSpace();
        String diskInfo = "OK (" + (freeBytes / (1024 * 1024 * 1024)) + " GB disponível)";

        String overallStatus = dbStatus.equals("UP") ? "UP" : "DOWN";

        HealthResponse response = new HealthResponse(
                overallStatus,
                dbStatus,
                diskInfo,
                LocalDateTime.now(),
                version
        );

        return ResponseEntity.ok(response);
    }
}

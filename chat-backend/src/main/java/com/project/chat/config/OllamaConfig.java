package com.project.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${rag.ollama.url:http://localhost:11434}")
    private String baseUrl;

    @Bean
    public HttpClient ollamaHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}

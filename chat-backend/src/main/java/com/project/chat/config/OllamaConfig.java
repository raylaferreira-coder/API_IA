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

    @Value("${rag.ollama.connect-timeout:10s}")
    private Duration connectTimeout;

    @Value("${rag.ollama.read-timeout:120s}")
    private Duration readTimeout;

    @Bean
    public HttpClient ollamaHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}

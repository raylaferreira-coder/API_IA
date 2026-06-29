package com.project.chat.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;

import com.project.chat.exception.EmbeddingException;

class EmbeddingServiceTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void constructor_ShouldLogStartupMessage() {
        assertDoesNotThrow(() -> new OllamaEmbeddingService("http://localhost:11434", "nomic-embed-text", httpClient));
    }

    @Test
    void embed_WhenOllamaUnavailable_ShouldThrowEmbeddingException() {
        OllamaEmbeddingService service = new OllamaEmbeddingService("http://localhost:99999", "nomic-embed-text",
                httpClient);

        assertThrows(EmbeddingException.class, () -> service.embed("teste"));
    }

    @Test
    void embed_WithLongText_ShouldTruncateTo8000Chars() {
        OllamaEmbeddingService service = new OllamaEmbeddingService("http://localhost:11434", "nomic-embed-text",
                httpClient);
        String longText = "a".repeat(10000);

        assertThrows(EmbeddingException.class, () -> service.embed(longText));
    }
}

package com.project.chat.service;

import com.project.chat.exception.EmbeddingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    @Test
    void constructor_ShouldLogStartupMessage() {
        assertDoesNotThrow(() -> new OllamaEmbeddingService("http://localhost:11434", "nomic-embed-text"));
    }

    @Test
    void embed_WhenOllamaUnavailable_ShouldThrowEmbeddingException() {
        OllamaEmbeddingService service = new OllamaEmbeddingService("http://localhost:99999", "nomic-embed-text");

        assertThrows(EmbeddingException.class, () -> service.embed("teste"));
    }

    @Test
    void embed_WithLongText_ShouldTruncateTo8000Chars() {
        OllamaEmbeddingService service = new OllamaEmbeddingService("http://localhost:11434", "nomic-embed-text");
        String longText = "a".repeat(10000);

        assertThrows(EmbeddingException.class, () -> service.embed(longText));
    }
}

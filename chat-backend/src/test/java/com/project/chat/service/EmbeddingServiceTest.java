package com.project.chat.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.chat.exception.EmbeddingException;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    void constructor_ShouldLogStartupMessage() {
        assertDoesNotThrow(() -> new OllamaEmbeddingService("http://localhost:11434", "nomic-embed-text", Duration.ofSeconds(120), mock(HttpClient.class)));
    }

    @Test
    void embed_WhenOllamaUnavailable_ShouldThrowEmbeddingException() throws IOException, InterruptedException {
        OllamaEmbeddingService service = new OllamaEmbeddingService("http://localhost:99999", "nomic-embed-text",
                Duration.ofSeconds(120), httpClient);

        assertThrows(EmbeddingException.class, () -> service.embed("teste"));
    }

    @Test
    void embed_WithLongText_ShouldTruncateTo8000Chars() throws IOException, InterruptedException {
        OllamaEmbeddingService service = new OllamaEmbeddingService("http://localhost:11434", "nomic-embed-text",
                Duration.ofSeconds(120), httpClient);
        String longText = "a".repeat(10000);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"embeddings\":[[0.1,0.2,0.3]]}");

        assertDoesNotThrow(() -> service.embed(longText));
    }
}

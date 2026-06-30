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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Profile("rag")
public class OllamaVisionService {

    private static final Logger log = LoggerFactory.getLogger(OllamaVisionService.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String visionModel;
    private final Duration readTimeout;
    private final ObjectMapper objectMapper;

    public OllamaVisionService(HttpClient httpClient,
                                @Value("${rag.ollama.url:http://localhost:11434}") String baseUrl,
                                @Value("${rag.ollama.vision-model:llava}") String visionModel,
                                @Value("${rag.ollama.read-timeout:120s}") Duration readTimeout) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.visionModel = visionModel;
        this.readTimeout = readTimeout;
        this.objectMapper = new ObjectMapper();
    }

    public String describeImage(byte[] imageBytes) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String url = baseUrl + "/api/generate";

            Map<String, Object> requestBody = Map.of(
                    "model", visionModel,
                    "prompt", "Descreva detalhadamente o conteudo desta imagem em portugues do Brasil.",
                    "stream", false,
                    "images", List.of(base64Image)
            );

            String jsonRequest = objectMapper.writeValueAsString(requestBody);

            log.debug("Enviando imagem para modelo de visao: model={}", visionModel);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(readTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama visao retornou erro {}: {}", response.statusCode(), response.body());
                return "[Modelo de visao indisponivel]";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            String result = (String) responseBody.get("response");

            log.debug("Descricao visual recebida: {} caracteres",
                    result != null ? result.length() : 0);

            return result != null ? result.trim() : "[Sem descricao disponivel]";

        } catch (Exception e) {
            log.error("Falha ao descrever imagem com modelo de visao: {}", e.getMessage());
            return "[Erro ao processar imagem: " + e.getMessage() + "]";
        }
    }
}

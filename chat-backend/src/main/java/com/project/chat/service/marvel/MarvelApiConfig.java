package com.project.chat.service.marvel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MarvelApiProperties.class)
public class MarvelApiConfig {

    @Bean
    public MarvelApiService marvelApiService(MarvelApiProperties properties) {
        return new MarvelApiService(properties);
    }

    @Bean
    public MarvelFandomIngestionService marvelFandomIngestionService(
            com.project.chat.service.EmbeddingService embeddingService,
            com.project.chat.service.ChunkService chunkService,
            com.project.chat.repository.DocumentRepository documentRepository,
            com.project.chat.repository.DocumentChunkRepository documentChunkRepository,
            com.project.chat.service.WebhookService webhookService) {
        return new MarvelFandomIngestionService(embeddingService, chunkService,
                documentRepository, documentChunkRepository, webhookService);
    }

    @Bean
    public MarvelWikipediaIngestionService marvelWikipediaIngestionService(
            com.project.chat.service.EmbeddingService embeddingService,
            com.project.chat.service.ChunkService chunkService,
            com.project.chat.repository.DocumentRepository documentRepository,
            com.project.chat.repository.DocumentChunkRepository documentChunkRepository,
            com.project.chat.service.WebhookService webhookService) {
        return new MarvelWikipediaIngestionService(embeddingService, chunkService,
                documentRepository, documentChunkRepository, webhookService);
    }
}

package com.project.chat.ai.config;

import com.project.chat.ai.MarvelAiAgent;
import com.project.chat.ai.client.OllamaClient;
import com.project.chat.ai.prompt.MarvelPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class AiAgentAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(AiAgentAutoConfig.class);

    @Bean
    @ConfigurationProperties(prefix = "ai.agent")
    public AiAgentProperties aiAgentProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        log.info("Carregando configuracoes do Agente de IA (modelo: {})", properties.getModel());
        return properties;
    }

    @Bean
    public OllamaClient ollamaClient(AiAgentProperties properties) {
        log.info("Configurando cliente Ollama em: {}", properties.getBaseUrl());
        return new OllamaClient(properties);
    }

    @Bean
    public MarvelPromptBuilder marvelPromptBuilder(AiAgentProperties properties) {
        log.info("Inicializando MarvelPromptBuilder com modelo: {}", properties.getModel());
        return new MarvelPromptBuilder(properties);
    }

    @Bean
    public MarvelAiAgent marvelAiAgent(OllamaClient ollamaClient,
                                       MarvelPromptBuilder promptBuilder,
                                       AiAgentProperties properties) {
        log.info("Inicializando MarvelAiAgent - Assistente especializado no Universo Marvel");
        log.info("Modelo LLM: {} | Modelo Embedding: {} | Ollama URL: {}",
                properties.getModel(), properties.getEmbeddingModel(), properties.getBaseUrl());
        return new MarvelAiAgent(ollamaClient, promptBuilder, properties);
    }
}

package com.project.chat.ai;

import com.project.chat.ai.client.OllamaClient;
import com.project.chat.ai.config.AiAgentProperties;
import com.project.chat.ai.dto.GenerateRequest;
import com.project.chat.ai.dto.GenerateResponse;
import com.project.chat.ai.prompt.MarvelPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarvelAiAgent implements AiAgent {

    private static final Logger log = LoggerFactory.getLogger(MarvelAiAgent.class);

    private final OllamaClient ollamaClient;
    private final MarvelPromptBuilder promptBuilder;
    private final AiAgentProperties properties;

    public MarvelAiAgent(OllamaClient ollamaClient,
                         MarvelPromptBuilder promptBuilder,
                         AiAgentProperties properties) {
        this.ollamaClient = ollamaClient;
        this.promptBuilder = promptBuilder;
        this.properties = properties;
    }

    @Override
    public String ask(String question) {
        log.info("MarvelAiAgent recebeu pergunta: {}", question);
        String prompt = promptBuilder.buildSimplePrompt(question);
        return executeQuery(prompt);
    }

    @Override
    public String askWithContext(String question, String context) {
        log.info("MarvelAiAgent recebeu pergunta com contexto ({} chars)", context.length());
        String prompt = promptBuilder.buildPromptWithContext(question, context);
        return executeQuery(prompt);
    }

    @Override
    public GenerateResponse askDetailed(String question) {
        log.info("MarvelAiAgent recebeu pergunta detalhada: {}", question);
        String prompt = promptBuilder.buildSimplePrompt(question);
        return executeDetailedQuery(prompt);
    }

    @Override
    public GenerateResponse askDetailedWithContext(String question, String context) {
        log.info("MarvelAiAgent recebeu pergunta detalhada com contexto ({} chars)", context.length());
        String prompt = promptBuilder.buildPromptWithContext(question, context);
        return executeDetailedQuery(prompt);
    }

    @Override
    public boolean isAvailable() {
        try {
            return ollamaClient.healthCheck();
        } catch (Exception e) {
            log.warn("Ollama indisponivel: {}", e.getMessage());
            return false;
        }
    }

    private String executeQuery(String prompt) {
        GenerateRequest request = new GenerateRequest(
                properties.getModel(),
                prompt,
                false,
                properties.getTemperature(),
                properties.getMaxTokens()
        );
        GenerateResponse response = ollamaClient.generate(request);
        return response.getResponse();
    }

    private GenerateResponse executeDetailedQuery(String prompt) {
        GenerateRequest request = new GenerateRequest(
                properties.getModel(),
                prompt,
                false,
                properties.getTemperature(),
                properties.getMaxTokens()
        );
        GenerateResponse response = ollamaClient.generate(request);
        response.setPrompt(prompt);
        response.setModelUsed(properties.getModel());
        return response;
    }
}

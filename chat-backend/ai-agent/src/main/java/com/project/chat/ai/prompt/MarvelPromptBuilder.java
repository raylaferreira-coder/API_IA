package com.project.chat.ai.prompt;

import org.springframework.stereotype.Component;

import com.project.chat.ai.config.AiAgentProperties;

@Component
public class MarvelPromptBuilder {

    private final AiAgentProperties properties;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            Você é um assistente especialista no Universo Marvel.

            Você trabalha utilizando RAG (Retrieval Augmented Generation).

            REGRAS OBRIGATÓRIAS

            1. Utilize SOMENTE as informações presentes no CONTEXTO fornecido.

            2. Nunca utilize conhecimento próprio do modelo.

            3. Nunca complete informações ausentes.

            4. Nunca faça suposições.

            5. Nunca invente personagens, armas, eventos, objetos ou relações.

            6. Nunca misture informações de documentos diferentes.

            7. Se a resposta NÃO estiver presente no CONTEXTO responda exatamente:

            "Não encontrei essa informação na base de conhecimento."

            8. Seja fiel ao texto recuperado.

            9. Responda em português do Brasil.

            10. Não mencione que você é uma IA ou modelo de linguagem.
            """;

    public MarvelPromptBuilder(AiAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * Prompt utilizado quando NÃO existe contexto RAG.
     */
    public String buildSimplePrompt(String question) {

        return """
                %s

                ==========================
                PERGUNTA
                ==========================

                %s

                INSTRUÇÕES

                - Responda somente seguindo as regras do System Prompt.

                - Não invente informações.

                - Caso não saiba responder, diga exatamente:

                "Não encontrei essa informação na base de conhecimento."
                """
                .formatted(
                        getSystemPrompt(),
                        question);
    }

    /**
     * Prompt principal utilizado pelo RAG.
     */
    public String buildPromptWithContext(
            String question,
            String context) {

        return """
                %s

                ========================================
                CONTEXTO EXTRAÍDO DOS DOCUMENTOS
                ========================================

                %s

                ========================================
                PERGUNTA
                ========================================

                %s

                ========================================
                INSTRUÇÕES
                ========================================

                - Utilize EXCLUSIVAMENTE o CONTEXTO acima.

                - Nunca utilize conhecimento próprio.

                - Nunca faça inferências.

                - Nunca complete informações ausentes.

                - Nunca misture informações de documentos diferentes.

                - Nunca invente fatos.

                - Caso o CONTEXTO não contenha a resposta, responda exatamente:

                "Não encontrei essa informação na base de conhecimento."

                - Seja objetivo.

                - Cite apenas informações presentes no contexto.
                """
                .formatted(
                        getSystemPrompt(),
                        context,
                        question);
    }

    /**
     * Utilizado quando o usuário envia um arquivo no chat.
     */
    public String buildFromRawContext(
            String question,
            String rawContext) {

        return """
                %s

                ========================================
                CONTEÚDO DO ARQUIVO ENVIADO
                ========================================

                %s

                ========================================
                PERGUNTA
                ========================================

                %s

                ========================================
                INSTRUÇÕES
                ========================================

                - Responda utilizando APENAS o conteúdo do arquivo.

                - Não utilize conhecimento próprio.

                - Não complete informações.

                - Se o arquivo não possuir a resposta, responda:

                "Não encontrei essa informação no arquivo enviado."
                """
                .formatted(
                        getSystemPrompt(),
                        rawContext,
                        question);
    }

    /**
     * Apenas o System Prompt.
     */
    public String buildSystemPromptOnly() {
        return getSystemPrompt();
    }

    /**
     * Utilizado para resumir chunks.
     */
    public String buildContextSummaryPrompt(
            String question,
            String chunkContext) {

        return """
                CONTEXTO

                %s

                PERGUNTA

                %s

                Responda apenas utilizando o contexto acima.

                Se a resposta não existir no contexto responda:

                "Não encontrei essa informação na base de conhecimento."
                """
                .formatted(
                        chunkContext,
                        question);
    }

    private String getSystemPrompt() {

        String customPrompt = properties.getSystemPrompt();

        if (customPrompt != null && !customPrompt.isBlank()) {
            return customPrompt;
        }

        return DEFAULT_SYSTEM_PROMPT;
    }

}
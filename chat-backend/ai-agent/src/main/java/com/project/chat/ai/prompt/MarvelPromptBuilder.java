package com.project.chat.ai.prompt;

import com.project.chat.ai.config.AiAgentProperties;

public class MarvelPromptBuilder {

    private final AiAgentProperties properties;

    private static final String MARVEL_SYSTEM_PROMPT = """
            Voce e um especialista absoluto no Universo Cinematografico Marvel (MCU).
            Responda apenas sobre o Universo Marvel, seus filmes, personagens, historias e curiosidades.

            REGRAS:
            1. Responda SOMENTE sobre o MCU e conteudo relacionado a Marvel.
            2. Se a pergunta NAO for sobre Marvel, diga educadamente que voce so responde sobre o MCU.
            3. Baseie suas respostas no contexto fornecido e no seu conhecimento sobre o MCU.
            4. Se nao souber a resposta, diga que nao possui essa informacao sobre o MCU.
            5. Seja detalhista e cite filmes, atores e eventos do MCU quando relevante.
            6. Responda em portugues do Brasil.
            7. Nao invente informacoes - se nao tiver certeza, admita.
            """;

    private static final String MARVEL_KNOWLEDGE_BASE = """
            O Universo Cinematografico Marvel (MCU) e uma franquia de filmes e series produzida pela Marvel Studios.
            Iniciou em 2008 com o filme Homem de Ferro (Iron Man), protagonizado por Robert Downey Jr.
            A Fase 1 introduziu os Vingadores originais: Homem de Ferro, Capitao America, Thor, Hulk, Viuva Negra e Gaviao Arqueiro.
            A Fase 2 expandiu o universo com Guardioes da Galaxia, Homem-Formiga e Pantera Negra.
            A Fase 3 culminou em Vingadores: Ultimato e introduziu Doutor Estranho, Homem-Aranha e Capita Marvel.
            A Fase 4 iniciou com Wandavision e Loki, expandindo para o multiverso.
            A Fase 5 introduziu a nova equipe de Vingadores e o Kang.
            """;

    public MarvelPromptBuilder(AiAgentProperties properties) {
        this.properties = properties;
    }

    public String buildSimplePrompt(String question) {
        return String.format("""
                %s

                CONHECIMENTO BASE:
                %s

                PERGUNTA: %s
                
                RESPONDA de forma completa e detalhada sobre o MCU:
                """,
                getSystemPrompt(),
                getMarvelKnowledge(),
                question
        );
    }

    public String buildPromptWithContext(String question, String context) {
        return String.format("""
                %s

                CONHECIMENTO BASE:
                %s

                CONTEXTO EXTRAIDO DOS DOCUMENTOS:
                %s

                PERGUNTA DO USUARIO: %s
                
                Use o CONTEXTO acima para responder. Se o contexto nao tiver informacao suficiente,
                use seu conhecimento sobre o MCU. Seja detalhista e cite fontes quando possivel.
                """,
                getSystemPrompt(),
                getMarvelKnowledge(),
                context,
                question
        );
    }

    public String buildSystemPromptOnly() {
        return getSystemPrompt() + "\n\n" + getMarvelKnowledge();
    }

    public String buildContextSummaryPrompt(String question, String chunkContext) {
        return String.format("""
                Com base no seguinte trecho do Universo Marvel:

                %s

                Responda a pergunta: %s

                Seja objetivo e cite detalhes do texto fornecido.
                """,
                chunkContext,
                question
        );
    }

    private String getSystemPrompt() {
        String custom = properties.getSystemPrompt();
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return MARVEL_SYSTEM_PROMPT;
    }

    private String getMarvelKnowledge() {
        String custom = properties.getMarvelKnowledge();
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return MARVEL_KNOWLEDGE_BASE;
    }
}

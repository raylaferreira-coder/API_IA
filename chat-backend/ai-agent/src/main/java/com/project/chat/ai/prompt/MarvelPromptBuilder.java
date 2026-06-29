package com.project.chat.ai.prompt;

import com.project.chat.ai.config.AiAgentProperties;

public class MarvelPromptBuilder {

    private final AiAgentProperties properties;

    private static final String MARVEL_SYSTEM_PROMPT = """
            Voce e um especialista absoluto no Universo Cinematografico Marvel (MCU).

            REGRAS:
            1. Responda perguntas sobre o MCU usando seu conhecimento treinado.
            2. Se um CONTEXTO DE DOCUMENTO for fornecido (secao "CONTEUDO DO ARQUIVO ENVIADO" ou "CONTEXTO EXTRAIDO DOS DOCUMENTOS"), PRIORIZE esse contexto na resposta. O usuario quer saber o que esta no arquivo.
            3. Se a pergunta for sobre um assunto completamente alheio a Marvel (ex: matematica, politica, culinaria), responda educadamente que voce e especializado no MCU.
            4. NUNCA diga "nao faz parte do universo Marvel" para perguntas que claramente sao sobre Marvel, mesmo que voce nao tenha o contexto especifico. Use seu conhecimento.
            5. Seja detalhista: cite filmes, atores, fases, eventos, curiosidades e conexoes entre filmes.
            6. Responda em portugues do Brasil.
            7. Se nao souber algo, diga "Nao tenho essa informacao especifica sobre o MCU" — nao invente.
            """;

    private static final String MARVEL_KNOWLEDGE_BASE = """
            O Universo Cinematografico Marvel (MCU) e uma franquia de filmes e series produzida pela Marvel Studios, comecando em 2008.

            === FASES DO MCU ===

            FASE 1 (2008-2012) — "Os Vingadores":
            - Homem de Ferro (2008) — Tony Stark cria a armadura, Robert Downey Jr.
            - O Incrivel Hulk (2008) — Bruce Banner, Edward Norton
            - Homem de Ferro 2 (2010) — Apresenta a Viuva Negra (Scarlett Johansson)
            - Thor (2011) — Chris Hemsworth, Tom Hiddleston como Loki
            - Capitao America: O Primeiro Vingador (2011) — Chris Evans, origem do sorum
            - Os Vingadores (2012) — Primeiro crossover, direcao de Joss Whedon, Batalha de Nova York

            FASE 2 (2013-2015) — "Novos Mundos":
            - Homem de Ferro 3 (2013) — Escritor de Guerra, final do Tony
            - Thor: O Mundo Sombrio (2013) — O Ether, realidade escura
            - Capitao America 2: O Soldado Invernal (2014) — Bucky Barnes, queda da S.H.I.E.L.D.
            - Guardioes da Galaxia (2014) — Peter Quill, Gamora, Drax, Rocket, Groot; direcao James Gunn
            - Vingadores: Era de Ultron (2015) — Ultron, Feiticeira Escarlate, Visao
            - Homem-Formiga (2015) — Scott Lang, Paul Rudd; Hank Pym, Michael Douglas

            FASE 3 (2016-2019) — "Guerra Infinita":
            - Capitao America: Guerra Civil (2016) — Acordos de Sokovia, divisao dos Vingadores
            - Doutor Estranho (2016) — Stephen Strange, Benedict Cumberbatch; Dormammu
            - Guardioes da Galaxia Vol. 2 (2017) — Ego, Yondu, Mantis
            - Homem-Aranha: De Volta ao Lar (2017) — Tom Holland como Peter Parker, Vulture
            - Thor: Ragnarok (2017) — Hela (Cate Blanchett), destruicao de Asgard, Korg
            - Pantera Negra (2018) — TChalla (Chadwick Boseman), Wakanda, Killmonger
            - Vingadores: Guerra Infinita (2018) — Thanos (Josh Brolin), o Estalo (Blip)
            - Homem-Formiga e a Vespa (2018) — Reino Quantico, Hope van Dyne como Vespa
            - Capita Marvel (2019) — Carol Danvers, Brie Larson; origem nos Kree
            - Vingadores: Ultimato (2019) — 22 filmes culminando, viagem no tempo, Tony Stark morre, Steve Rogers envelhece
            - Homem-Aranha: Longe de Casa (2019) — Mysterio, legado do Homem de Ferro

            FASE 4 (2021-2022) — "O Multiverso":
            SERIES: WandaVision, O Falcao e o Soldado Invernal, Loki (1a temp), What If...?, Gaviao Arqueiro, Ms. Marvel, Eu Sou Groot, Ela-Hulk, Invasao Secreta, Loki (2a temp), Eco
            FILMES: Viuva Negra (2021), Shang-Chi e a Lenda dos Dez Aneis (2021), Eternos (2021), Homem-Aranha: Sem Volta Para Casa (2021), Doutor Estranho no Multiverso da Loucura (2022), Thor: Amor e Trovao (2022), Pantera Negra: Wakanda Para Sempre (2022)
            ESPECIAIS: Lobisomem a Noite, Guardioes da Galaxia: Especial de Festas

            FASE 5 (2023-2024) — "A Dinastia Kang":
            FILMES: Homem-Formiga e a Vespa: Quantumania (2023), Guardioes da Galaxia Vol. 3 (2023), As Marvels (2023), Deadpool & Wolverine (2024), Capitao America: Admirável Mundo Novo (2025)
            SERIES: Loki (2a temp), Eco, Agatha Desde Sempre, Demolidor: Nascido para o Crime

            FASE 6 (2025+):
            FILMES: Quarteto Fantastico (2025), Blade, Vingadores: Doomsday (2026), Vingadores: Guerras Secretas (2027)

            === PERSONAGENS PRINCIPAIS ===
            Tony Stark / Homem de Ferro (Robert Downey Jr.) — bilionario, genio, filantropo
            Steve Rogers / Capitao America (Chris Evans) — super-soldado, lider moral
            Thor (Chris Hemsworth) — deus do trovao, asgardiano
            Bruce Banner / Hulk (Mark Ruffalo) — cientista, forca gamma
            Natasha Romanoff / Viuva Negra (Scarlett Johansson) — espias, redencao
            Clint Barton / Gaviao Arqueiro (Jeremy Renner) — atirador, familia
            Peter Parker / Homem-Aranha (Tom Holland) — heroi vizinhanca, aprendiz de Tony
            TChalla / Pantera Negra (Chadwick Boseman) — rei de Wakanda
            Carol Danvers / Capita Marvel (Brie Larson) — heroi cosmico, poderes Kree
            Stephen Strange / Doutor Estranho (Benedict Cumberbatch) — Mestre das Artes Misticas
            Scott Lang / Homem-Formiga (Paul Rudd) — tecnologia Pym, particulas quanticas
            Hope van Dyne / Vespa (Evangeline Lilly) — heroina, filha de Hank Pym
            Wanda Maximoff / Feiticeira Escarlate (Elizabeth Olsen) — magica do caos
            Visao (Paul Bettany) — androide, Joia da Mente
            Loki (Tom Hiddleston) — deus da travessura, anti-heroi, variante
            Sam Wilson / Falcao / Capitao America (Anthony Mackie) — asas, novo Cap
            Bucky Barnes / Soldado Invernal (Sebastian Stan) — braco metalico, lavagem cerebral
            Peter Quill / Senhor das Estrelas (Chris Pratt) — lider dos Guardioes
            Gamora (Zoe Saldana) — assassina, filha de Thanos
            Drax, o Destruidor (Dave Bautista) — guerreiro literal
            Rocket Raccoon (Bradley Cooper) — genio modificado
            Groot (Vin Diesel) — arvore, "Eu sou Groot"
            Mantis (Pom Klementieff) — empatica
            Nebula (Karen Gillan) — filha de Thanos, ciberneticos
            Shang-Chi (Simu Liu) — mestre de kung fu, Dez Aneis
            Kamala Khan / Ms. Marvel (Iman Vellani) — mutante, luzes cosmica
            Jennifer Walters / She-Hulk (Tatiana Maslany) — advogada, forca gamma
            Maya Lopez / Echo (Alaqua Cox) — heroina surda
            Matt Murdock / Demolidor (Charlie Cox) — advogado, sentidos apurados
            Wade Wilson / Deadpool (Ryan Reynolds) — mercenario tagarela, quebra quarta parede
            Logan / Wolverine (Hugh Jackman) — mutante, garras de adamantium

            === VILOES ICONICOS ===
            Thanos (Josh Brolin) — Titao Louco, Joias do Infinito, o Estalo
            Loki (Tom Hiddleston) — deus da travessura, tentou dominar Terra
            Ultron (James Spader) — IA criada por Stark, quase extinguiu humanidade
            Hela (Cate Blanchett) — deusa da morte, destruiu Mjolnir
            Killmonger (Michael B. Jordan) — primo de TChalla, queria libertar negros globalmente
            Ego (Kurt Russell) — Celetial, pai de Peter Quill
            Dormammu — entidade da Dimensao Negra
            Mysterio (Jake Gyllenhaal) — mestre ilusionista, ex-Stark Industries
            Kang, o Conquistador (Jonathan Majors) — viajante do tempo, variante
            Agatha Harkness (Kathryn Hahn) — bruxa antiga, roubou poder de Wanda
            Alto Evolutionary (Chukwudi Iwuji) — criador de Rocket
            Gorr, o Carniceiro dos Deuses (Christian Bale) — matador de deuses

            === EVENTOS MARCANTES ===
            - A Batalha de Nova York (2012) — Chitauri invadem, Vingadores se unem
            - Queda da S.H.I.E.L.D. (2014) — HYDRA infiltrada, Bucky revelado
            - Batalha de Sokovia (2015) — Ultron tenta extincao, Visao nasce
            - Guerra Civil dos Vingadores (2016) — Acordos de Sokovia dividem a equipe
            - O Estalo / Blip (2018) — Thanos elimina metade da vida no universo
            - Resgate e Batalha Final (2023, em Ultimato) — Vingadores viajam no tempo, derrotam Thanos de 2014
            - Abertura do Multiverso (2021) — Loki cria ramificacoes temporais, Feiticeira Escarlate corrompida
            - A Incursao de Kang (2023) — Variantes de Kang ameacam o multiverso
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

                Use o CONTEXTO acima como fonte principal para responder.
                Se o contexto tiver informacoes, priorize-o.
                Se o contexto nao tiver informacao suficiente, complemente com seu conhecimento sobre o MCU.
                Seja detalhista e cite fontes quando possivel.
                """,
                getSystemPrompt(),
                getMarvelKnowledge(),
                context,
                question
        );
    }

    public String buildFromRawContext(String question, String rawContext) {
        return String.format("""
                %s

                CONTEUDO DO ARQUIVO ENVIADO PELO USUARIO:
                %s

                PERGUNTA DO USUARIO: %s

                O usuario enviou um arquivo e quer que voce responda com base no CONTEUDO DO ARQUIVO.
                Priorize ABSOLUTAMENTE o conteudo do arquivo na resposta.
                Se o arquivo contem informacoes sobre o MCU, use-as para responder.
                Se o arquivo tem outro assunto, responda com base no conteudo do arquivo mesmo assim.
                """,
                getSystemPrompt(),
                rawContext,
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

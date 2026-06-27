# Agente de IA - Marvel MCU

Pasta dedicada exclusivamente ao **Agente de Inteligencia Artificial** especializado no Universo Cinematografico Marvel (MCU).

## Arquitetura do Agente

```
ai-agent/
├── README.md
├── docker/
│   └── ollama-gemma4.yml              # Docker para Ollama com Gemma 4
├── src/
│   └── main/
│       ├── java/com/project/chat/ai/
│       │   ├── AiAgent.java            # Interface do agente
│       │   ├── MarvelAiAgent.java      # Implementacao Marvel
│       │   ├── config/
│       │   │   ├── AiAgentAutoConfig.java  # Auto-configuracao Spring
│       │   │   └── AiAgentProperties.java  # Propriedades do agente
│       │   ├── client/
│       │   │   └── OllamaClient.java       # Cliente HTTP para Ollama
│       │   ├── prompt/
│       │   │   └── MarvelPromptBuilder.java # Construtor de prompts MCU
│       │   └── dto/
│       │       ├── GenerateRequest.java
│       │       ├── GenerateResponse.java
│       │       ├── EmbeddingRequest.java
│       │       └── EmbeddingResponse.java
│       └── resources/
│           └── application-ai.yml      # Configuracao do agente
```

## Tecnologias

| Componente | Tecnologia |
|------------|-----------|
| Modelo LLM  | **Gemma 4** (Google via Ollama) |
| Embedding   | nomic-embed-text |
| Servidor    | Ollama (localhost:11434) |
| Framework   | Spring Boot (auto-configuravel) |
| Cliente HTTP| Java HttpClient nativo |

## Como Usar

### 1. Iniciar o Ollama com Gemma 4

```bash
cd ai-agent/docker
docker-compose -f ollama-gemma4.yml up -d
```

> Aguarde o download dos modelos (pode levar alguns minutos na primeira execucao).

### 2. Verificar se os modelos estao prontos

```bash
curl http://localhost:11434/api/tags
```

### 3. Configurar no Spring Boot

Adicione no `application.yml` do projeto principal:

```yaml
spring:
  profiles:
    include: ai
```

Ou ative o perfil ao iniciar:

```bash
java -jar app.jar --spring.profiles.active=ai
```

### 4. Usar o agente no codigo

```java
@Autowired
private MarvelAiAgent marvelAgent;

public void exemplo() {
    String resposta = marvelAgent.ask("Quem e o Homem de Ferro?");
    System.out.println(resposta);
}
```

## API do Agente

### Interface `AiAgent`

| Metodo | Descricao |
|--------|-----------|
| `ask(question)` | Pergunta simples, retorna texto |
| `askWithContext(question, context)` | Pergunta com contexto extra |
| `askDetailed(question)` | Pergunta detalhada, retorna objeto completo |
| `askDetailedWithContext(question, context)` | Detalhada com contexto |
| `isAvailable()` | Verifica se o Ollama esta online |

## Exemplo de Uso

```java
MarvelAiAgent agent = context.getBean(MarvelAiAgent.class);

// Pergunta simples
System.out.println(agent.ask("Qual a pedra mais poderosa do infinito?"));

// Com contexto RAG
String contexto = "A Joia da Alma foi revelada em Vingadores: Guerra Infinita...";
System.out.println(agent.askWithContext("Onde esta a Joia da Alma?", contexto));
```

## Variaveis de Ambiente

| Variavel | Padrao | Descricao |
|----------|--------|-----------|
| `AI_AGENT_BASE_URL` | `http://localhost:11434` | URL do Ollama |
| `AI_AGENT_MODEL` | `gemma4` | Modelo LLM |
| `AI_AGENT_EMBEDDING_MODEL` | `nomic-embed-text` | Modelo de embedding |
| `AI_AGENT_TEMPERATURE` | `0.7` | Temperatura do modelo |
| `AI_AGENT_MAX_TOKENS` | `2048` | Maximo de tokens |
| `AI_AGENT_ENABLED` | `true` | Ativar/desativar agente |

# Technical Debt — Chat RAG Marvel

## Classificação

| Prioridade | Descrição |
|---|---|
| **CRÍTICO** | Impede deploy, causa perda de dados, falha de segurança, impossível de dar manutenção |
| **ALTO** | Viola princípios fundamentais, causa bugs frequentes, performance degradada |
| **MÉDIO** | Código difícil de manter, duplicação moderada, viola boas práticas |
| **BAIXO** | Cosmético, melhoria futura, sem impacto funcional |

---

## 1. God Objects

### 1.1 `RagChatService` — **CRÍTICO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/service/RagChatService.java`

**Problema:** Esta classe possui 15+ dependências injetadas no construtor e acumula todas as responsabilidades do domínio de chat:

| Responsabilidade | Justificativa |
|---|---|
| Validação de sessão | Deveria estar em SessionValidator |
| Criação/reuso de conversa | Deveria estar em ConversationService |
| Salvamento de mensagens | Já existe no repositório |
| Fluxo RAG completo (embed → retrieve → prompt → generate) | Lógica central, mas acoplada |
| Upload-and-ask com OCR + Vision | Deveria ser delegado |
| Streaming SSE (cria SseEmitter, gerencia ciclo de vida) | Lógica de infra no service |
| Async processing (ExecutorService) | Gerenciamento de threads |
| Visão computacional (OCR + descrição de imagem) | Domínio distinto |

**Impacto:** Dificuldade extrema de testar, modificar ou entender. Qualquer mudança no fluxo de chat exige alteração nesta classe gigante.

**Sugestão:** Extrair para:
- `SessionValidator` (validação de sessão)
- `ConversationManager` (criação/reuso de conversas)
- `MessageHandler` (salvamento de mensagens)
- `RagPipelineService` (embed → retrieve → prompt → generate)
- `AsyncChatHandler` (async + streaming)
- `VisionChatHandler` (upload-and-ask com OCR/Vision)

---

### 1.2 `MarvelFandomIngestionService` — **ALTO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/service/marvel/MarvelFandomIngestionService.java`

**Problema:** Combina scraping HTML (com parser manual de tags), extração de título, extração de conteúdo, pipeline de ingestão (chunk → embed → save → webhook) e gerenciamento de URLs de categoria.

**Impacto:** 300+ linhas com múltiplas responsabilidades. Difícil de testar o scraping sem executar o pipeline completo.

**Sugestão:** Extrair `FandomHtmlScraper` (scraping + extração) e usar `DocumentIngestionService.processDocument()` para o pipeline.

---

## 2. Violações de SOLID

### 2.1 SRP — `RagChatService` — **CRÍTICO**

Já documentado em [God Objects](#11-ragchatservice--crítico).

### 2.2 SRP — `MarvelIngestionController` — **CRÍTICO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/controller/MarvelIngestionController.java`

**Problema:** O controller injeta diretamente `DocumentRepository`, `DocumentChunkRepository`, `EmbeddingService`, `ChunkService`, `WebhookService` e contém a lógica completa de ingestão Marvel API no método `ingestFromMarvelApi()` (linha ~130-170). Viola a regra 1 do `AGENTS.md`: "Controllers não contêm regra de negócio — apenas delegam para Services."

**Código violador:**
```java
@Transactional
@PostMapping("/ingest/marvel-api")
public ResponseEntity<MarvelIngestionResponse> ingestFromMarvelApi() {
    // ... lógica de negócio: chunking, embedding, salvamento, webhook
}
```

**Sugestão:** Mover toda a lógica para `MarvelApiIngestionService` e deixar o controller apenas delegar.

### 2.3 DIP — `MarvelPromptBuilder` com conhecimento hardcoded — **MÉDIO**

**Arquivo:** `chat-backend/ai-agent/src/main/java/com/project/chat/ai/prompt/MarvelPromptBuilder.java`

**Problema:** O conhecimento do MCU (~400 linhas de texto enciclopédico) está hardcoded como constantes `static final String` na classe. Isso viola o DIP porque a classe concreta contém dados que deveriam vir de uma fonte externa (arquivo, banco, API).

**Impacto:** Para atualizar o conhecimento do MCU, é necessário modificar o código-fonte e recompilar.

**Sugestão:** Externalizar para `marvel-knowledge.md` no classpath ou tabela no banco.

### 2.4 ISP — `ChatService` interface inchada — **ALTO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/service/ChatService.java`

**Problema:** A interface `ChatService` possui 7 métodos que misturam responsabilidades distintas:
- Mensagem síncrona
- Mensagem assíncrona
- Mensagem streaming
- Upload-and-ask
- Histórico
- Consulta de conversa
- Status de task

`SimulatedChatService` é forçado a implementar métodos que poderiam ser default ou separados.

**Sugestão:** Separar em interfaces menores:
- `ChatMessaging` (sendMessage, sendMessageAsync, sendMessageStream)
- `ChatHistory` (getHistory, getConversation)
- `ChatUpload` (uploadAndAsk)
- `TaskTracker` (getTaskStatus)

---

## 3. Acoplamento Excessivo

### 3.1 `DocumentIngestionService` retorna `Document` e expõe `searchSimilar` — **ALTO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/service/DocumentIngestionService.java`

**Problema:** O serviço de ingestão expõe métodos de busca (`searchSimilar`, `searchSimilarByDocument`) que são responsabilidade de `DocumentQueryService` / `VectorRetrievalService`. Isso cria acoplamento desnecessário entre ingestão e consulta.

**Métodos duplicados:**
- `DocumentIngestionService.searchSimilar()` — duplica `DocumentQueryService.searchSimilar()` e `VectorRetrievalService.search()`
- `DocumentIngestionService.searchSimilarByDocument()` — sem equivalente em outro lugar

### 3.2 `ConversationMapper` depende de `MessageMapper` — **MÉDIO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/mapper/ConversationMapper.java`

**Problema:** Acoplamento desnecessário. `ConversationMapper` poderia simplesmente chamar `MessageMapper` diretamente ou receber o mapper já resolvido.

### 3.3 `MessageMapper.toEntity()` faz `attachmentRepository.findById()` — **ALTO**

**Arquivo:** `chat-backend/src/main/java/com/project/chat/mapper/MessageMapper.java`

**Problema:** Mapper não deveria ter lógica de banco de dados. O mapper é responsável por conversão de tipos, não por busca de entidades.

```java
public Message toEntity(ChatRequest request, Conversation conversation, MessageRole role) {
    Message message = new Message(conversation, role, request.getContent());
    if (request.getAttachmentId() != null) {
        attachmentRepository.findById(request.getAttachmentId())  // ← REGRA DE NEGÓCIO
            .ifPresent(attachment -> {
                message.setAttachment(attachment);
                attachment.setMessage(message);
            });
    }
    return message;
}
```

---

## 4. Duplicação de Código

### 4.1 Pipeline chunking+embedding+save replicado — **CRÍTICO**

O mesmo padrão aparece em 5 lugares diferentes:

| Local | Arquivo |
|---|---|
| `DocumentIngestionService.processDocument()` | `service/DocumentIngestionService.java` |
| `MarvelIngestionController.ingestFromMarvelApi()` | `controller/MarvelIngestionController.java` |
| `MarvelFandomIngestionService.ingestSinglePage()` | `service/marvel/MarvelFandomIngestionService.java` |
| `MarvelWikipediaIngestionService.ingestSinglePage()` | `service/marvel/MarvelWikipediaIngestionService.java` |
| `RagChatService.uploadAndAsk()` | `service/RagChatService.java` (parcial) |

**Padrão duplicado:**
```java
List<String> chunks = chunkService.chunkText(text);
List<float[]> embeddings = embeddingService.embedAll(chunks);
List<DocumentChunk> chunkEntities = new ArrayList<>();
for (int i = 0; i < chunks.size(); i++) {
    DocumentChunk chunk = new DocumentChunk(doc, i, chunks.get(i), ...);
    if (i < embeddings.size()) chunk.setEmbedding(embeddings.get(i));
    chunkEntities.add(chunk);
}
documentChunkRepository.saveAll(chunkEntities);
doc.setTotalChunks(chunks.size());
```

### 4.2 Validação de sessão + criação de conversa replicada — **ALTO**

O mesmo bloco de ~40 linhas aparece em:
- `RagChatService.sendMessage()`
- `RagChatService.uploadAndAsk()`
- `SimulatedChatService.sendMessage()`
- `SimulatedChatService.uploadAndAsk()`

### 4.3 `ImageOcrParser` vs `RagChatService` detecção de imagem — **MÉDIO**

`ImageOcrParser` tem `Set<String> IMAGE_TYPES` e `RagChatService` tem `Set<String> IMAGE_TYPES` com os mesmos valores. Também em `DocumentController.SUPPORTED_INGESTION_TYPES`.

### 4.4 `FileUtils.MAX_FILE_SIZE` vs `DocumentController.MAX_INGESTION_FILE_SIZE` — **MÉDIO**

#### Arquivos:
- `util/FileUtils.java`: `MAX_FILE_SIZE = 50L * 1024 * 1024`
- `controller/DocumentController.java`: `MAX_INGESTION_FILE_SIZE = 50L * 1024 * 1024`

Valores idênticos (50MB), mas definidos em dois lugares.

---

## 5. Classes Muito Grandes

| Classe | Linhas | Problema |
|---|---|---|
| `RagChatService` | ~500+ (arquivo truncado) | 15+ dependências, múltiplas responsabilidades |
| `MarvelPromptBuilder` | ~400 (conhecimento hardcoded) | Dados deveriam ser externos |
| `MarvelFandomIngestionService` | ~300 | Scraping + ingestão acoplados |
| `MarvelIngestionController` | ~180 | Controller com regra de negócio |
| `GlobalExceptionHandler` | ~200 | 15 métodos de @ExceptionHandler |

---

## 6. Métodos Muito Grandes

| Método | Arquivo | Linhas | Problema |
|---|---|---|---|
| `MarvelIngestionController.ingestFromMarvelApi()` | marvel/MarvelIngestionController.java | ~50 | Pipeline completo no controller |
| `RagChatService.sendMessage()` | service/RagChatService.java | ~120 | Sessão + conversa + mensagem + RAG |
| `RagChatService.uploadAndAsk()` | service/RagChatService.java | ~100 | Upload + OCR + Vision + RAG |
| `MarvelFandomIngestionService.extractContent()` | marvel/MarvelFandomIngestionService.java | ~60 | Parser HTML manual inline |
| `HealthController.health()` | controller/HealthController.java | ~60 | Tudo inline sem delegar |

---

## 7. Dependências Desnecessárias

### 7.1 `PdfTextExtractor` (deprecated) — **BAIXO**

**Arquivo:** `util/PdfTextExtractor.java`

Wrapper deprecated que encapsula `PdfParser`. Marcado como `@Deprecated` mas ainda presente. Remove após verificação de usos.

### 7.2 AI Agent Module não integrado — **MÉDIO**

**Arquivo:** `chat-backend/ai-agent/`

O módulo `AiAgent` + `MarvelAiAgent` + `OllamaClient` duplica funcionalidade do `OllamaChatService`. O fluxo RAG real usa `OllamaChatService` + `MarvelPromptBuilder` diretamente, ignorando o `MarvelAiAgent`.

Isso adiciona complexidade desnecessária ao build (incluso via `build-helper-maven-plugin`) e manutenção duplicada de client HTTP para Ollama.

---

## 8. Possíveis Gargalos de Performance

### 8.1 Embeddings sequenciais com `parallelStream()` limitado — **ALTO**

**Arquivo:** `service/OllamaEmbeddingService.java`

```java
public List<float[]> embedAll(List<String> texts) {
    for (int i = 0; i < texts.size(); i += MAX_PARALLEL) {
        List<String> batch = texts.subList(i, end);
        results.addAll(batch.parallelStream().map(this::embed).toList());
    }
}
```

**Problemas:**
- `parallelStream()` usa `ForkJoinPool.commonPool()` — afeta todo o sistema
- `MAX_PARALLEL=2` limita severamente a vazão em lotes grandes
- Cada chamada `embed()` é bloqueante (HTTP síncrono)
- Sem timeout ou circuit breaker por chamada individual

**Documentos com muitos chunks (ex: 50 chunks) processam em 25 iterações de 2.**

### 8.2 Webhook síncrono bloqueia ingestão — **ALTO**

**Arquivo:** `service/DocumentIngestionService.java`

`webhookService.notify()` é chamado síncronamente dentro do método `processDocument()`. Se o n8n estiver lento ou indisponível, toda a ingestão fica bloqueada.

Com retry configurável (3 tentativas com 1s delay), pode adicionar até 3+ segundos à resposta.

### 8.3 `DocumentRepository.findAll()` sem paginação — **MÉDIO**

**Arquivo:** `service/DocumentQueryService.java`

```java
public DocumentsListResponse listDocuments() {
    List<Document> documents = documentRepository.findAll();  // ← sem Pageable
}
```

Com muitos documentos, essa consulta pode retornar milhares de registros e causar `OutOfMemoryError`.

### 8.4 `ConversationRepository.findBySessionSessionIdOrderByUpdatedAtDesc()` com EntityGraph — **MÉDIO**

Carrega todas as mensagens de cada conversa via `@EntityGraph("Conversation.withMessages")` apenas para contar mensagens e pegar a última. Bastaria uma consulta count + subquery.

### 8.5 Thread pools não configurados — **MÉDIO**

`RagChatService` cria `Executors.newFixedThreadPool(10)` para streaming e async sem configurar:
- Nome das threads (difícil debugar)
- Tamanho da fila
- Política de rejeição
- Shutdown hook

---

## 9. Problemas de Concorrência

### 9.1 `ExecutorService` nunca fechado — **CRÍTICO**

**Arquivo:** `service/RagChatService.java`

```java
private final ExecutorService streamingExecutor = Executors.newFixedThreadPool(10);
private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
```

Nenhum dos dois executors tem shutdown. Em restart ou redeploy, as threads continuam ativas (vazamento de memória).

### 9.2 `TaskService` sem garantias de consistência — **ALTO**

**Arquivo:** `service/TaskService.java`

Tasks armazenadas em `ConcurrentHashMap`:
- Perdidas em restart
- Sem expiração automática
- Podem acumular indefinidamente
- Tamanho não limitado (possível memory leak em uso intenso)

### 9.3 `OllamaEmbeddingService.embedAll()` sem sincronização — **MÉDIO**

Múltiplas chamadas concorrentes a `embedAll()` disputam o `ForkJoinPool.commonPool()` e o Ollama pode receber mais requisições do que consegue processar, causando timeout.

### 9.4 `SseEmitter` sem timeout — **MÉDIO**

**Arquivo:** `service/RagChatService.java`

```java
SseEmitter emitter = new SseEmitter();  // timeout padrão?
```

O timeout padrão do SseEmitter pode ser inadequado para respostas longas do Ollama.

---

## 10. Problemas de Arquitetura

### 10.1 Controller com regra de negócio e transação — **CRÍTICO**

`MarvelIngestionController.ingestFromMarvelApi()` tem `@Transactional` e pipeline completo de ingestão. Viola o princípio arquitetural fundamental do projeto.

### 10.2 Perfis Spring não padronizados — **ALTO**

| Perfil | Uso |
|---|---|
| `dev` | Simulado (SimulatedChatService + NoopRetrievalService) |
| `rag` | Real (RagChatService + OllamaChatService + VectorRetrievalService) |
| `prod` | Real (mesmas classes que rag, configs diferentes) |

**Problema:** O perfil `rag` é o default e também o usado em produção (via docker-compose). Não há distinção clara entre homologação e produção. Perfil `prod` nunca é ativado pelo docker-compose.

### 10.3 AI Agent Module isolado e duplicado — **MÉDIO**

O módulo `ai-agent` replica:
- `OllamaClient` → idêntico em propósito a `OllamaChatService`
- `MarvelPromptBuilder` → idêntico ao `PromptBuilder` + `MarvelPromptBuilder` do módulo principal
- `AiAgent` + `MarvelAiAgent` → encapsulamento que poderia ser integrado ao `ChatService`

O `PromptBuilder` do módulo principal já injeta `MarvelPromptBuilder` do módulo `ai-agent`, criando dependência bidirecional entre módulos.

### 10.4 Duas formas de fazer busca semântica — **MÉDIO**

| Método | Local |
|---|---|
| `DocumentIngestionService.searchSimilar(query, limit)` | Chama `embed()` + `findSimilarChunks()` |
| `VectorRetrievalService.search(vector, topK)` | Mesma lógica |
| `DocumentQueryService.searchSimilar(query, topK)` | Chama `embed()` + `findSimilarChunks()` + calcula similaridade |

Três implementações diferentes para a mesma operação.

### 10.5 `Document` entity com nome confuso — **MÉDIO**

**Arquivo:** `entity/Document.java`

| Coluna banco | Nome getter | O que representa |
|---|---|---|
| `title` | `getFileName()` | Nome do arquivo OU título da URL |
| `source_url` | `getSourcePath()` | Path do arquivo OU URL de origem |

Os nomes dos getters não correspondem ao que os campos realmente armazenam, causando confusão.

### 10.6 `DatabaseInitializer` duplica migration SQL — **BAIXO**

**Arquivo:** `config/DatabaseInitializer.java`

```java
jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ...");
```

O mesmo índice é definido em `db/migration/V1__create_vector_extension.sql`. A migration é ignorada porque `ddl-auto=update` no JPA substitui Flyway.

### 10.7 Tratamento de erro silencioso no OCR — **ALTO**

**Arquivo:** `parser/ImageOcrParser.java`

```java
catch (TesseractException e) {
    log.warn("OCR falhou: ...");
    return "";  // ← silencia erro
} catch (Exception e) {
    log.warn("Erro inesperado: ...");
    return "";  // ← silencia erro
}
```

Se o Tesseract não estiver instalado, o OCR falha silenciosamente e retorna string vazia. O fluxo continua sem que o usuário saiba que o OCR falhou.

---

## Resumo por Prioridade

| Prioridade | Quantidade | Exemplos |
|---|---|---|
| **CRÍTICO** | 6 | RagChatService God Object, MarvelIngestionController com regra de negócio, pipeline replicado, Executor sem shutdown, sessão/conversa duplicada |
| **ALTO** | 10 | SRP violado múltiplas vezes, acoplamento excessivo, webhook síncrono, sem paginação, task em memória, perfil não padronizado, ChatService inchado |
| **MÉDIO** | 12 | DIP violado, mappers com regra de negócio, AiAgent isolado, embedAll sem rate limit, constantes duplicadas, nomes de entidade confusos |
| **BAIXO** | 3 | PdfTextExtractor deprecated, DatabaseInitializer duplicado, formatação |
| **TOTAL** | **31** | |

## Itens Prioritários para Ação Imediata

| # | Item | Prioridade | Esforço Estimado |
|---|---|---|---|
| 1 | Extrair regra de negócio do `MarvelIngestionController` | CRÍTICO | 2h |
| 2 | Adicionar `@PreDestroy` para shutdown dos Executors | CRÍTICO | 30min |
| 3 | Extrair pipeline replicado para método comum em `DocumentIngestionService` | CRÍTICO | 4h |
| 4 | Extrair `SessionValidator` e `ConversationManager` | ALTO | 4h |
| 5 | Tornar webhook assíncrono (`@Async`) | ALTO | 2h |
| 6 | Adicionar paginação em `DocumentQueryService.listDocuments()` | ALTO | 1h |
| 7 | Remover `attachmentRepository.findById()` de `MessageMapper` | ALTO | 1h |
| 8 | Externalizar conhecimento Marvel do `MarvelPromptBuilder` | MÉDIO | 3h |

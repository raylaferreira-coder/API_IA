# Arquitetura Futura — Chat RAG Marvel v3

---

## 1. Arquitetura Atual

A arquitetura atual é uma **monolito modular** Spring Boot 3.4.5 com organização em pacotes por camada técnica:

```
controller → service → repository → entity
         ↘ parser / marvel ↗
```

**Características dominantes:**
- Camadas técnicas (controller, service, repository, entity)
- Serviços ricos que acumulam regras de negócio e orquestração (RagChatService como God Object)
- Perfis Spring para alternar entre simulado (`dev`) e real (`rag`)
- Strategy Pattern para parsers (`ParserFactory` + `DocumentParser`)
- DTOs separados em `request/` e `response/`
- Exception hierarchy + `GlobalExceptionHandler`
- AI Agent Module como módulo Maven separado via `build-helper-maven-plugin`

**Principais gargalos arquiteturais identificados em TECHNICAL_DEBT.md:**
- RagChatService: 15+ dependências, 6+ responsabilidades distintas
- Pipeline chunk+embed+save replicado em 5 lugares
- MarvelIngestionController com regras de negócio
- Executors não gerenciados (vazamento de threads)
- Webhook síncrono bloqueando ingestão
- AI Agent Module isolado e duplicando funcionalidade

---

## 2. Problemas da Arquitetura Atual

| # | Problema | Impacto |
|---|---|---|
| P1 | Acoplamento service→repository direto | Testabilidade comprometida, viola DIP |
| P2 | God Objects (RagChatService) | Manutenção cara, bugs frequentes |
| P3 | Pipeline replicado (chunk+embed+save) | Correção de bugs requer alterar N lugares |
| P4 | Controller com regra de negócio (MarvelIngestionController) | Viola Clean Architecture |
| P5 | Mapper com regra de banco (MessageMapper.toEntity) | Viola SRP |
| P6 | Executors sem shutdown | Vazamento de recursos |
| P7 | Webhook síncrono | Latência desnecessária |
| P8 | Perfis Spring misturam simulação e produção | Config confusa |
| P9 | Domínio anêmico (entidades sem comportamento) | Lógica espalhada nos services |
| P10 | Sem portas/adaptadores explícitos | Trocar implementação exige alterar services |

---

## 3. Arquitetura Alvo — Hexagonal Modular

```
                        ┌──────────────────────────────────┐
                        │           CLIENTES               │
                        │    (REST, SSE, WebSocket)        │
                        └──────────────┬───────────────────┘
                                       │
                        ┌──────────────▼───────────────────┐
                        │         API LAYER                │
                        │  Controllers  │  DTOs            │
                        │  Exception   │  Validators       │
                        │  Handlers    │  Mappers          │
                        └──────────────┬───────────────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            │           APPLICATION LAYER (Use Cases)            │
            │                                                      │
            │  ┌───────────┐ ┌──────────┐ ┌───────────────────┐  │
            │  │ Chat      │ │ Document │ │ Marvel            │  │
            │  │ UseCases  │ │ UseCases │ │ UseCases          │  │
            │  └─────┬─────┘ └────┬─────┘ └────────┬──────────┘  │
            │        │             │                 │             │
            │  ┌──────▼─────────────▼─────────────────▼────────┐ │
            │  │           Event Publisher / Bus               │ │
            │  └──────────────────────┬───────────────────────┘ │
            └─────────────────────────┼──────────────────────────┘
                                      │
            ┌─────────────────────────┼──────────────────────────┐
            │          DOMAIN LAYER (Ports + Entities)          │
            │                                                      │
            │  ┌──────────┐ ┌───────────┐ ┌────────────────┐   │
            │  │ Chat     │ │ Document  │ │ LLM            │   │
            │  │ Domain   │ │ Domain    │ │ Ports          │   │
            │  │ Entities │ │ Entities  │ │ (interfaces)   │   │
            │  │ + Events │ │ + Events  │ │                │   │
            │  └────┬─────┘ └─────┬─────┘ └───────┬────────┘   │
            │       │             │                │            │
            │  ┌─────▼─────────────▼────────────────▼────────┐ │
            │  │         Domain Events / Integration Events  │ │
            │  └──────────────────────┬──────────────────────┘ │
            └─────────────────────────┼──────────────────────────┘
                                      │
            ┌─────────────────────────┼──────────────────────────┐
            │      INFRASTRUCTURE LAYER (Adapters)              │
            │                                                      │
            │  ┌──────────┐ ┌──────────┐ ┌────────────────┐   │
            │  │ JPA      │ │ pgvector │ │ Ollama         │   │
            │  │ Adapter  │ │ Adapter  │ │ Adapters       │   │
            │  ├──────────┤ ├──────────┤ ├────────────────┤   │
            │  │ File     │ │ Parser   │ │ Webhook        │   │
            │  │ Storage  │ │ Adapters │ │ Adapter        │   │
            │  ├──────────┤ ├──────────┤ ├────────────────┤   │
            │  │ Marvel   │ │ Fandom   │ │ Wikipedia      │   │
            │  │ API      │ │ Scraper  │ │ API            │   │
            │  └──────────┘ └──────────┘ └────────────────┘   │
            └──────────────────────────────────────────────────┘
```

### Princípios da Arquitetura Alvo

| Princípio | Aplicação |
|---|---|
| **Clean Architecture** | Dependências apontam para dentro (domain não sabe de infra) |
| **Hexagonal (Ports & Adapters)** | Interfaces no domínio, implementações na infra |
| **DDD** | Bounded Contexts: Chat, Document, Knowledge, LLM |
| **SOLID** | SRP estrito, DIP via portas, ISP em interfaces finas |
| **Event-Driven** | Eventos de domínio para desacoplar contextos |
| **Modular Monolith** | Módulos Maven com boundaries claros, deployável como um JAR |

---

## 4. Novos Módulos

```
chat-backend/
├── api/                          # Interfaces de entrada (REST)
├── application/                  # Casos de uso (orquestração)
├── chat-core/                    # Domínio de chat (entidades + portas)
├── document-engine/              # Processamento de documentos
├── rag-engine/                   # Pipeline RAG (embed → retrieve → prompt)
├── llm-engine/                   # Abstração de LLM (porta + adapters)
├── vision-engine/                # OCR e visão computacional
├── knowledge-engine/             # Ingestão e gerenciamento de base de conhecimento
├── marvel-plugin/                # Plugin Marvel (API + Fandom + Wikipedia)
├── shared-kernel/                # Tipos compartilhados, exceções, utilidades
├── infrastructure/               # Adaptadores concretos (JPA, pgvector, HTTP, etc.)
└── core/                         # Bootstrap Spring Boot, configuração global
```

---

## 5. Responsabilidade de Cada Módulo

### 5.1 `chat-core`

**O que contém:**
- Entidades: `Session`, `Conversation`, `Message`, `Attachment`
- Value Objects: `SessionId`, `MessageRole`, `ConversationTitle`
- Domain Events: `MessageSentEvent`, `ConversationCreatedEvent`, `SessionExpiredEvent`
- Port interfaces: `SessionRepository`, `ConversationRepository`, `MessageRepository`, `AttachmentRepository`
- Domain Services: `ConversationDomainService` (regras de título, data de expiração)

**O que NÃO contém:**
- Controllers, DTOs, JPA, HTTP clients, transações Spring

**Dependências:** `shared-kernel` apenas

**Regra:** Nenhuma dependência de framework. Java puro.

---

### 5.2 `rag-engine`

**O que contém:**
- Entidades: `RetrievalResult`, `RagContext`
- Domain Events: `QueryExecutedEvent`
- Port interfaces: `RagPipeline` (porta única de entrada)
- Services: `RagOrchestrator` (orquestra embed → retrieve → rerank → prompt)
- Value Objects: `RelevanceScore`, `ChunkWithScore`

**O que NÃO contém:**
- Implementação de embedding (delega para `llm-engine`)
- Implementação de retrieval (delega para `infrastructure`)
- Implementação de LLM (delega para `llm-engine`)

**Dependências:** `chat-core`, `llm-engine` (portas apenas), `shared-kernel`

---

### 5.3 `document-engine`

**O que contém:**
- Entidades: `Document`, `DocumentChunk`, `DocumentMetadata`
- Value Objects: `DocumentStatus`, `SourceType`, `ChunkContent`, `ChunkIndex`
- Domain Events: `DocumentIngestedEvent`, `DocumentFailedEvent`
- Port interfaces: `DocumentRepository`, `DocumentChunkRepository`, `DocumentParser`
- Domain Services: `ChunkingService` (regras de chunking com overlap)

**O que NÃO contém:**
- Implementações de parser (ficam em `infrastructure/parser`)
- Controllers REST
- Conhecimento de Marvel ou fontes específicas

**Dependências:** `shared-kernel`, `llm-engine` (porta EmbeddingService)

---

### 5.4 `vision-engine`

**O que contém:**
- Entidades: `ImageAnalysisResult`
- Port interfaces: `OcrService`, `VisionService`
- Domain Services: `ImageAnalysisOrchestrator` (tenta OCR, fallback para Vision)

**O que NÃO contém:**
- Implementação de Tesseract OCR (fica em `infrastructure`)
- Implementação de Vision LLM (fica em `llm-engine`)

**Dependências:** `shared-kernel`, `llm-engine` (porta VisionService)

---

### 5.5 `llm-engine`

**O que contém:**
- Port interfaces: `ChatLlmService`, `EmbeddingService`, `VisionService`
- Value Objects: `LlmPrompt`, `LlmResponse`, `EmbeddingVector`, `ModelConfig`
- Domain Events: `LlmCallCompletedEvent`
- Domain Services: `PromptBuilder` (template de prompts)

**O que NÃO contém:**
- Implementação de HTTP client (fica em `infrastructure`)
- Configuração de modelos específicos
- Conhecimento de Marvel (fica em `marvel-plugin`)

**Dependências:** `shared-kernel`

---

### 5.6 `knowledge-engine`

**O que contém:**
- Entidades: `KnowledgeSource`, `KnowledgeEntry`
- Port interfaces: `KnowledgeSourceProvider` (para plugins de fonte)
- Services: `KnowledgeIngestionOrchestrator`, `KnowledgeSearchService`
- Domain Events: `KnowledgeSyncCompletedEvent`

**O que NÃO contém:**
- Implementação de fontes específicas (fica em plugins como `marvel-plugin`)
- Implementação de embedding (delega para `llm-engine`)

**Dependências:** `document-engine`, `llm-engine`, `rag-engine`, `shared-kernel`

---

### 5.7 `marvel-plugin`

**O que contém:**
- Implementação de `KnowledgeSourceProvider` para Marvel
- `MarvelApiClient` (usa portas HTTP de `infrastructure`)
- `FandomScraper` (usa portas HTTP de `infrastructure`)
- `WikipediaApiClient` (usa portas HTTP de `infrastructure`)
- `MarvelPromptEnricher` (conhecimento específico do MCU)

**O que NÃO contém:**
- Regras de ingestão genéricas (usa `knowledge-engine`)
- Controllers REST
- Pipeline de chunking/embedding (usa `document-engine` + `llm-engine`)

**Dependências:** `knowledge-engine` (portas), `llm-engine`, `shared-kernel`

**Caráter opcional:** Pode ser removido sem afetar os módulos core.

---

### 5.8 `shared-kernel`

**O que contém:**
- Exceções base: `DomainException`, `NotFoundException`, `ValidationException`
- Tipos utilitários: `Either<L,R>`, `Result<T>`
- Anotações: `@UseCase`, `@Port`, `@Adapter`
- Constantes compartilhadas
- `BaseEntity` (id + timestamps)

**Dependências:** Nenhuma (zero dependencies)

---

### 5.9 `api`

**O que contém:**
- Controllers REST (apenas delegação para use cases)
- DTOs de request/response
- Mappers (DTO ↔ Domain)
- `GlobalExceptionHandler`
- Validators (Bean Validation + custom)

**O que NÃO contém:**
- Regras de negócio
- Acesso a repositórios
- Anotações JPA

**Dependências:** `application`, `shared-kernel`

---

### 5.10 `infrastructure`

**O que contém:**
- Adaptadores JPA: implementam portas de `chat-core` e `document-engine`
- Adaptador pgvector: busca por similaridade
- Adaptadores Ollama: implementam `ChatLlmService`, `EmbeddingService`, `VisionService`
- Adaptadores de parser: `PdfParser`, `HtmlParser`, `TxtParser`, `MarkdownParser`, `WordParser`, `UrlParser`, `ImageOcrAdapter`
- Adaptador FileStorage: implementa porta de armazenamento
- Adaptador Webhook: notificação n8n
- Adaptadores HTTP: cliente genérico para APIs externas
- Configuração JPA/Hibernate, Flyway migrations

**O que NÃO contém:**
- Interfaces/portas (definidas nos módulos de domínio)
- Regras de negócio
- Entidades de domínio (usa as do domínio)

**Dependências:** Todos os módulos de domínio (portas), `shared-kernel`

---

### 5.11 `core`

**O que contém:**
- `ChatApplication.java` (main class)
- `@Configuration`, `@Bean` definitions
- Property sources, profile config
- Component scanning configuration
- Event bus configuration

**Dependências:** Todos os outros módulos

---

## 6. Fluxo Completo — Mensagem

```
┌──────┐   ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌─────────┐   ┌──────────┐   ┌──────────┐
│Client│   │api       │   │application   │   │chat-core │   │rag-engine│   │llm-engine│   │infra    │
│      │   │Controller│   │SendMessageUC │   │domain    │   │          │   │(portas)  │   │adapters │
└──┬───┘   └────┬─────┘   └──────┬───────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘
   │POST /chat   │               │                │              │             │              │
   │/message     │               │                │              │             │              │
   ├──────────>│               │                │              │             │              │
   │            │ execute(dto)  │                │              │             │              │
   │            ├──────────────>│                │              │             │              │
   │            │               │ valida sessão  │              │             │              │
   │            │               │────────────────────────────────────────────->│              │
   │            │               │<─────────────────────────────────────────────│              │
   │            │               │               │              │             │              │
   │            │               │ cria/reativa  │              │             │              │
   │            │               │   conversa    │              │             │              │
   │            │               │─────────────────────────────>│              │             │
   │            │               │<─────────────────────────────│              │             │
   │            │               │               │              │             │              │
   │            │               │ salva msg     │              │             │              │
   │            │               │   USER        │              │             │              │
   │            │               │───────────────────────────────────────────────────────────>│
   │            │               │<───────────────────────────────────────────────────────────│
   │            │               │               │              │             │              │
   │            │               │               │   INÍCIO RAG  │             │              │
   │            │               │               │              │             │              │
   │            │               │ embed(text)   │              │             │              │
   │            │               │───────────────────────────────────────────────────────────>│
   │            │               │               │              │             │ POST /api/   │
   │            │               │               │              │             │   embed      │
   │            │               │<───────────────────────────────────────────────────────────│
   │            │               │               │              │ float[768]  │              │
   │            │               │               │              │             │              │
   │            │               │ search(vec,5) │              │             │              │
   │            │               │───────────────────────────────────────────────────────────>│
   │            │               │               │              │             │ SELECT <->   │
   │            │               │<───────────────────────────────────────────────────────────│
   │            │               │               │              │ 5 chunks    │              │
   │            │               │               │              │             │              │
   │            │               │ buildPrompt() │              │             │              │
   │            │               │──────────────────────────────>│             │              │
   │            │               │<──────────────────────────────│             │              │
   │            │               │               │              │             │              │
   │            │               │ generate(p)   │              │             │              │
   │            │               │───────────────────────────────────────────────────────────>│
   │            │               │               │              │             │ POST /api/   │
   │            │               │               │              │             │   generate   │
   │            │               │<───────────────────────────────────────────────────────────│
   │            │               │               │              │  resposta   │              │
   │            │               │               │              │             │              │
   │            │               │           FIM RAG             │             │              │
   │            │               │               │              │             │              │
   │            │               │ salva msg     │              │             │              │
   │            │               │   ASSISTANT   │              │             │              │
   │            │               │───────────────────────────────────────────────────────────>│
   │            │               │<───────────────────────────────────────────────────────────│
   │            │               │               │              │             │              │
   │            │               │ publica evento│              │             │              │
   │            │               │ MessageSent   │─────────────>│             │              │
   │            │<──────────────│               │              │             │              │
   │<───────────│               │               │              │             │              │
```

### Eventos disparados assincronamente após resposta:
- `MessageSentEvent` → `WebhookHandler` → notifica n8n
- `MessageSentEvent` → `AnalyticsHandler` → métricas de uso
- `MessageSentEvent` → `AuditHandler` → log de auditoria

---

## 7. Fluxo Upload + Ingestão

```
┌──────┐  ┌──────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  ┌──────────┐
│Client│  │api       │  │application   │  │document-    │  │vision-engine │  │llm-engine │  │infra    │
│      │  │Controller│  │IngestDocUC   │  │engine       │  │              │  │(portas)   │  │adapters │
└──┬───┘  └────┬─────┘  └──────┬───────┘  └──────┬──────┘  └──────┬───────┘  └─────┬─────┘  └────┬─────┘
   │POST/doc   │               │                 │               │              │              │
   │/ingest    │               │                 │               │              │              │
   │──file────>│               │                 │               │              │              │
   │           │ execute(file) │                 │               │              │              │
   │           ├──────────────>│                 │               │              │              │
   │           │               │ cria Document   │               │              │              │
   │           │               │─────────────────────────────────────────────────────────────>│
   │           │               │<─────────────────────────────────────────────────────────────│
   │           │               │ id=123          │               │              │              │
   │           │               │ status=PENDING  │               │              │              │
   │           │               │                 │               │              │              │
   │           │               │ store(file)     │               │              │              │
   │           │               │─────────────────────────────────────────────────────────────>│
   │           │               │<─────────────────────────────────────────────────────────────│
   │           │               │                 │               │              │              │
   │           │               │ determina       │               │              │              │
   │           │               │   tipo          │               │              │              │
   │           │               │                 │               │              │              │
   │           │               │ ┌─ É imagem? ──│──────────────>│              │              │
   │           │               │ │              │               │              │              │
   │           │               │ │ ocr(img)     │               │              │              │
   │           │               │ │───────────────────────────────────────────────────────────>│
   │           │               │ │              │               │              │  Tesseract    │
   │           │               │ │<─────────────│───────────────│──────────────│──────────────│
   │           │               │ │              │               │              │              │
   │           │               │ │ Se OCR vazio │               │              │              │
   │           │               │ │ describe(img)│               │              │              │
   │           │               │ │───────────────────────────────────────────────────────────>│
   │           │               │ │              │               │              │  Ollama Vision│
   │           │               │ │<─────────────│───────────────│──────────────│──────────────│
   │           │               │ │              │               │              │              │
   │           │               │ └─ rawText ───>│               │              │              │
   │           │               │                 │               │              │              │
   │           │               │ chunkText(raw)  │               │              │              │
   │           │               │────────────────>│               │              │              │
   │           │               │<────────────────│               │              │              │
   │           │               │   List<String>  │               │              │              │
   │           │               │                 │               │              │              │
   │           │               │ embedAll(chunks)│               │              │              │
   │           │               │─────────────────────────────────────────────────────────────>│
   │           │               │<─────────────────────────────────────────────────────────────│
   │           │               │   List<float[]> │               │              │              │
   │           │               │                 │               │              │              │
   │           │               │ salva Chunks    │               │              │              │
   │           │               │─────────────────────────────────────────────────────────────>│
   │           │               │<─────────────────────────────────────────────────────────────│
   │           │               │                 │               │              │              │
   │           │               │ publica evento  │               │              │              │
   │           │               │ DocumentIngested│               │              │              │
   │           │               │──(async)─────────────────────────────────────────────────────>│
   │           │               │                 │               │              │   Webhook    │
   │           │               │                 │               │              │   n8n        │
   │           │<──────────────│                 │               │              │              │
   │<──────────│ 202 ACCEPTED  │                 │               │              │              │
```

**Nota:** O webhook é disparado **assincronamente** via evento, não bloqueia a resposta.

---

## 8. Dependências Permitidas

### 8.1 Regra Fundamental

```
api ──> application ──> domain (ports) <── infrastructure (adapters)
                              ↕
                        shared-kernel
```

### 8.2 Matriz de Dependências

| Módulo | Pode depender de | NÃO pode depender de |
|---|---|---|
| `api` | application, shared-kernel | infrastructure, domínio diretamente |
| `application` | chat-core, document-engine, rag-engine, llm-engine, vision-engine, knowledge-engine, shared-kernel | api, infrastructure |
| `chat-core` | shared-kernel | api, application, infrastructure, qualquer adapter |
| `document-engine` | shared-kernel, llm-engine (portas) | api, application, infrastructure |
| `rag-engine` | chat-core, llm-engine (portas), shared-kernel | api, application, infrastructure |
| `llm-engine` | shared-kernel | api, application, infrastructure |
| `vision-engine` | shared-kernel, llm-engine (portas) | api, application, infrastructure |
| `knowledge-engine` | document-engine, llm-engine, rag-engine, shared-kernel | api, infrastructure |
| `marvel-plugin` | knowledge-engine (portas), llm-engine (portas), shared-kernel | api, application, infrastructure |
| `infrastructure` | **TODOS** os módulos de domínio (portas), shared-kernel | api, application |
| `core` | **TODOS** os módulos | N/A (top-level) |

### 8.3 Regras de Dependência

```
api → application → domain ← infrastructure
                      ↓
                shared-kernel (todos dependem)
```

**Diagrama de dependência simplificado:**

```
      api
       │
       ▼
 application ──────────────────────────────┐
       │                                   │
       ▼                                   ▼
 ┌──────────┐  ┌──────────────┐  ┌──────────────────┐
 │chat-core  │  │document-engine│  │   rag-engine     │
 │           │  │               │  │                  │
 │Session    │  │Document       │  │RagOrchestrator   │
 │Conversat. │  │DocumentChunk  │  │RetrievalResult   │
 │Message    │  │DocumentStatus  │  └───────┬──────────┘
 └──────────┘  └───────┬───────┘           │
                       │                   │
 ┌──────────┐  ┌───────▼───────┐  ┌───────▼──────────┐
 │vision-   │  │llm-engine     │  │knowledge-engine  │
 │engine    │  │               │  │                  │
 │OcrPort   │  │ChatLlmPort    │  │KnowledgeSource   │
 │VisionPort│  │EmbeddingPort  │  │IngestionOrch.    │
 └──────────┘  │VisionPort     │  └───────┬──────────┘
               └───────┬───────┘          │
                       │                  ▼
                 ┌──────▼───────┐  ┌──────────────┐
                 │ shared-kernel│  │ marvel-plugin│
                 └──────────────┘  └──────────────┘
                       │                  │
                       ▼                  ▼
                 ┌───────────────────────────────────┐
                 │        infrastructure             │
                 │  JPA | pgvector | Ollama | etc   │
                 └───────────────────────────────────┘
```

---

## 9. Regras Arquiteturais

### 9.1 Controllers (api)

```
REGRAS:
✅ Apenas anotar @RestController, @RequestMapping, @Valid
✅ Delegar para ApplicationService imediatamente
✅ Usar DTOs de request/response (nunca expor entidades de domínio)
✅ Tratar exceções via GlobalExceptionHandler
❌ NÃO conter regras de negócio
❌ NÃO injetar repositórios
❌ NÃO conter anotações @Transactional
❌ NÃO acessar HttpSession, HttpRequest diretamente (usar argumentos)
```

### 9.2 Application Services (application)

```
REGRAS:
✅ Orquestrar fluxos: chamar portas de domínio e coordenar resultados
✅ Gerenciar transações (@Transactional no nível de use case)
✅ Publicar eventos de domínio após operações bem-sucedidas
✅ Mapear exceções de domínio para respostas apropriadas
❌ NÃO conter lógica de negócio (delegar para domínio)
❌ NÃO conter anotações de infraestrutura (JPA, HTTP)
❌ NÃO depender de implementações concretas (apenas portas)
```

### 9.3 Domain Entities (chat-core, document-engine, etc.)

```
REGRAS:
✅ Encapsular comportamento junto com dados
✅ Publicar DomainEvents quando estado relevante muda
✅ Validar invariantes no construtor/setters
✅ Usar Value Objects para conceitos tipados
❌ NÃO ter anotações de framework (JPA, Jackson)
❌ NÃO ter dependências de infraestrutura
❌ NÃO expor setters públicos (imutabilidade preferencial)
```

### 9.4 Ports (chat-core, llm-engine, etc.)

```
REGRAS:
✅ Definir interfaces no módulo de domínio
✅ Usar tipos de domínio nos parâmetros/retornos
✅ Nomear pelo propósito (não pela tecnologia): DocumentRepository,
   não JpaDocumentRepository
❌ NÃO ter sufixo "Impl" na definição
❌ NÃO depender de tecnologia específica
```

### 9.5 Adapters (infrastructure)

```
REGRAS:
✅ Implementar portas definidas no domínio
✅ Conter toda a tecnologia: JPA, HTTP clients, file I/O
✅ Mapear entre tipos de infra e tipos de domínio
✅ Ser substituível: trocar JPA por MongoDB = novo adapter apenas
❌ NÃO expor classes de infra para outros módulos
❌ NÃO vazar exceções de tecnologia (SQLException → DomainException)
```

### 9.6 DTOs (api)

```
REGRAS:
✅ Usar records do Java (ou classes finais imutáveis)
✅ Conter anotações de validação (jakarta.validation)
✅ Ser específicos para cada caso de uso (não reutilizar)
❌ NÃO conter lógica de negócio
❌ NÃO referenciar entidades de domínio
```

### 9.7 Eventos

```
REGRAS:
✅ Eventos de domínio: imutáveis, nomeados no passado (MessageSent)
✅ Handlers: processam assincronamente, falham isoladamente
✅ Publicação: via port (DomainEventPublisher) — infra decide implementação
✅ Cada handler é uma classe separada (SRP)
❌ NÃO acoplar handlers entre si
❌ NÃO usar eventos para fluxos síncronos (use cases resolvem)
```

### 9.8 Módulos Maven

```
REGRAS:
✅ Um módulo Maven por contexto delimitado
✅ Dependências apenas para módulos mais internos (nunca para fora)
✅ Ciclo de dependência proibido (compilador falha)
✅ Testes unitários no mesmo módulo
✅ Testes de integração no módulo infrastructure
```

---

## 10. Roadmap de Migração

### Fase 1 — Fundação (Semanas 1-2)

```
Objetivo: Estabelecer estrutura de módulos sem quebrar nada.

Passos:
1. Criar estrutura Maven multi-módulo
   ├── chat-backend/
   │   ├── pom.xml (parent)
   │   ├── shared-kernel/
   │   ├── chat-core/
   │   ├── llm-engine/
   │   ├── api/
   │   ├── application/
   │   ├── infrastructure/
   │   ├── core/
   │   └── ... (demais módulos vazios inicialmente)

2. Mover para shared-kernel:
   - Exceções base (DomainException → renomear)
   - FileUtils, PdfTextExtractor
   - Constantes compartilhadas

3. Definir portas em chat-core:
   - SessionRepository, ConversationRepository
   - MessageRepository, AttachmentRepository

4. Mover entidades para chat-core:
   - Session, Conversation, Message, Attachment
   - Remover anotações JPA (criar contrapartes JPA em infra)

Estratégia de compatibilidade:
  - Manter classes originais como delegadas (facade)
  - Package scan inclui ambos os locais
  - Nenhum endpoint quebra
```

### Fase 2 — Chat Core (Semanas 3-4)

```
Objetivo: Extrair RagChatService e SimulatedChatService.

Passos:
1. Criar use cases em application/chat:
   - SendMessageUseCase
   - SendMessageStreamUseCase
   - SendMessageAsyncUseCase
   - UploadAndAskUseCase
   - GetHistoryUseCase

2. Extrair SessionValidator para chat-core domain service

3. Extrair ConversationManager para chat-core domain service

4. Implementar MessageHandler como port + adapter

5. Criar adaptadores JPA em infrastructure:
   - JpaSessionRepository (implementa SessionRepository)
   - JpaConversationRepository
   - JpaMessageRepository
   - JpaAttachmentRepository

6. Mover @Transactional para ApplicationService

Estratégia de compatibilidade:
  - RagChatService vira fachada que delega para use cases
  - Perfis Spring continuam funcionando (dev/rag)
```

### Fase 3 — RAG + Document Engine (Semanas 5-6)

```
Objetivo: Extrair rag-engine e document-engine.

Passos:
1. Mover ChunkService para document-engine (regras puras)

2. Extrair pipeline de ingestão:
   - IngestDocumentUseCase (application)
   - DocumentIngestionService fica em document-engine
   - MarvelIngestionController perde lógica → vira fachada

3. Criar rag-engine:
   - RagOrchestrator (orquestra embed → retrieve → prompt)
   - Port EmbeddingService (interface) no llm-engine
   - Port RetrievalService (interface) no rag-engine

4. Criar adaptadores em infrastructure:
   - OllamaEmbeddingAdapter
   - PgVectorRetrievalAdapter

5. Unificar busca semântica:
   - Eliminar DocumentIngestionService.searchSimilar()
   - Eliminar DocumentQueryService.searchSimilar()
   - Manter apenas VectorRetrievalService → renomear para PgVectorRetrievalAdapter

Estratégia de compatibilidade:
  - Interfaces antigas viram @Deprecated
  - Gradualmente mover chamadas para novas portas
```

### Fase 4 — Vision + LLM Engine (Semanas 7-8)

```
Objetivo: Extrair vision-engine e llm-engine.

Passos:
1. Criar llm-engine module:
   - Port ChatLlmService (generate, generateStream)
   - Port EmbeddingService (embed, embedAll)
   - Port VisionService (describeImage)
   - Value Objects: LlmConfig, ModelType, Prompt

2. Criar vision-engine module:
   - Port OcrService
   - ImageAnalysisOrchestrator (OCR primeiro, Vision fallback)
   - Independência de Tesseract ou LLM

3. Criar adaptadores:
   - OllamaChatAdapter
   - OllamaEmbeddingAdapter
   - OllamaVisionAdapter
   - TesseractOcrAdapter

4. Remover código duplicado:
   - Eliminar OllamaChatService antigo (vira OllamaChatAdapter)
   - Eliminar OllamaEmbeddingService antigo
   - Eliminar OllamaVisionService antigo
   - Eliminar ImageOcrParser antigo

Estratégia de compatibilidade:
  - Novos adaptadores implementam interfaces antigas também
  - Remoção gradual com @Deprecated
```

### Fase 5 — Knowledge Engine + Marvel Plugin (Semanas 9-10)

```
Objetivo: Extrair knowledge-engine e marvel-plugin.

Passos:
1. Criar knowledge-engine:
   - Port KnowledgeSourceProvider
   - KnowledgeIngestionOrchestrator
   - Usa document-engine e llm-engine internamente

2. Criar marvel-plugin:
   - MarvelApiProvider (implementa KnowledgeSourceProvider)
   - FandomScraperProvider
   - WikipediaApiProvider
   - MarvelPromptEnricher (conhecimento MCU externalizado)

3. Externalizar conhecimento Marvel:
   - Mover constantes de MarvelPromptBuilder para arquivo YAML/JSON
   - Carregar via @ConfigurationProperties
   - Possibilitar atualização sem recompilar

4. Refatorar MarvelIngestionController:
   - Remover toda regra de negócio
   - Apenas delegar para KnowledgeIngestionUseCase

Estratégia de compatibilidade:
  - marvel-plugin é opcional no classpath
  - Sistema funciona sem ele (apenas sem conhecimento Marvel)
```

### Fase 6 — Resiliência + Observabilidade (Semanas 11-12)

```
Objetivo: Tornar pronto para produção.

Passos:
1. Webhook assíncrono:
   - ApplicationEventPublisher para DocumentIngestedEvent
   - WebhookEventHandler processa em thread separada
   - Retry com backoff exponencial

2. Gerenciamento de threads:
   - Substituir Executors.newFixedThreadPool por ThreadPoolTaskExecutor
   - @Bean com configuração explícita (pool name, queue, rejection)
   - @PreDestroy para shutdown graceful

3. Circuit Breaker para Ollama:
   - Adicionar resilience4j-spring-boot3
   - CircuitBreaker nas chamadas de embedding e chat
   - TimeLimiter para evitar hangs

4. Métricas:
   - Micrometer + Actuator
   - Métricas por use case: tempo, taxa de erro, tokens
   - Métricas de ingestão: documentos/hora, chunks, tamanho

5. Health checks aprimorados:
   - HealthController usa indicadores do Spring Boot Actuator
   - Ping individual por dependência (DB, Ollama, n8n)

6. Rate Limiter:
   - Semáforo para chamadas de embedding (evitar sobrecarga do Ollama)
   - Configurável via application.yml
```

---

## 11. Riscos

| # | Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|---|
| R1 | **Quebra de compatibilidade de API** durante migração | Média | Alto | Manter endpoints antigos como @Deprecated, versionamento de API (`/v1/`, `/v2/`) |
| R2 | **Perda de dados** ao migrar entidades JPA | Baixa | Crítico | Scripts de migração testados, backup obrigatório, modo shadow read |
| R3 | **Performance degradada** com camadas adicionais de abstração | Média | Médio | Benchmark antes/depois, profiler (Async Profiler), cache onde necessário |
| R4 | **Complexidade cognitiva** aumenta com eventos assíncronos | Alta | Médio | Documentação clara, diagramas de sequência, rastreabilidade com correlationId |
| R5 | **Injeção de dependência circular** entre módulos | Média | Alto | Regra arquitetural de dependências unidirecionais, verificação em CI |
| R6 | **Tempo de migração subestimado** | Alta | Alto | MVP em 6 semanas, funcionalidades completas em 12, iterações curtas |
| R7 | **Resistência da equipe** a nova arquitetura | Média | Médio | Pair programming, code reviews, workshops de Hexagonal Architecture |
| R8 | **Over-engineering** para cenário atual | Média | Baixo | Módulo marvel-plugin é opcional, começar extraindo apenas o que causa dor |
| R9 | **Eventual consistency** introduz bugs difíceis de rastrear | Baixa | Alto | Event sourcing apenas para contextos críticos, testes de integração com Kafka/event bus |
| R10 | **Dependência `build-helper-maven-plugin`** para módulo ai-agent | Baixa | Baixo | Incorporar ai-agent ao módulo principal ou transformar em módulo Maven real |

---

## 12. Benefícios

### 12.1 Manutenibilidade

| Antes | Depois |
|---|---|
| RagChatService com 500+ linhas e 15 dependências | RagOrchestrator com ~50 linhas, 3 dependências |
| Pipeline replicado em 5 lugares | Pipeline único em document-engine |
| Controller com regra de negócio | Controller apenas delega |
| Mapper com regra de banco | Mapper apenas converte |
| Para adicionar parser: implementar + registrar | Para adicionar parser: implementar interface + colocar no classpath |

### 12.2 Testabilidade

| Antes | Depois |
|---|---|
| Testar RagChatService requer mockar 15 dependências | Testar SendMessageUseCase requer mockar 2-3 portas |
| Teste de parser requer banco de dados | Teste de parser usa apenas InputStream (teste unitário puro) |
| Teste de integração depende de Docker | Portas podem ser mockadas, adaptadores testados isoladamente |

### 12.3 Extensibilidade

| Cenário | Arquitetura Atual | Arquitetura Alvo |
|---|---|---|
| Novo parser (ex: EPUB) | Adicionar classe + modificar ParserFactory | Implementar DocumentParser + colocar no classpath |
| Novo LLM (ex: Claude) | Criar novo service + perfil Spring | Implementar ChatLlmPort + configurar via @Profile |
| Novo webhook (ex: Slack) | Modificar WebhookService | Implementar WebhookPort + registrar handler do evento |
| Nova fonte de conhecimento (ex: DC Comics) | Copiar FandomIngestionService | Implementar KnowledgeSourceProvider + marvel-plugin 2 |
| Trocar PostgreSQL por MongoDB | Reescrever repositories + SQL nativo | Implementar nova persistência (MongoDocumentRepo) + injetar |

### 12.4 Performance

| Antes | Depois |
|---|---|
| Webhook síncrono bloqueia resposta 1-3s | Webhook assíncrono (resposta em <100ms) |
| Executors sem shutdown (vazamento) | ThreadPoolTaskExecutor gerenciado pelo Spring |
| Sem rate limit no Ollama | Circuit Breaker + semáforo de embedding |
| Sem paginação em listDocuments | Pageable obrigatório |

### 12.5 Qualidade de Código

| Métrica | Antes | Depois (esperado) |
|---|---|---|
| Acoplamento (dependências por classe) | 15+ (RagChatService) | <5 por use case |
| Coesão (responsabilidades por classe) | 6+ (RagChatService) | 1 por use case |
| Cobertura de testes | ~17 testes | >80% por módulo |
| Duplicação | Pipeline em 5 lugares | Zero |
| Violações SOLID | Múltiplas (ver TECHNICAL_DEBT.md) | Zero |
| Tempo para adicionar funcionalidade | Dias | Horas |

### 12.6 Preparação para Produção

| Aspecto | Antes | Depois |
|---|---|---|
| Observabilidade | Apenas logs | Métricas + health checks + tracing |
| Resiliência | Sem proteção | Circuit breaker + retry + timeout |
| Escalabilidade | Monolito vertical | Módulos podem ser extraídos para microsserviços |
| Segurança | URL validation básico | Validação em cada adapter |
| Configuração | Duplicada entre profiles | Hierarquia clara (default → profile → env) |
| Documentação | AGENTS.md + README | Código auto-documentado + ADRs + diagramas |

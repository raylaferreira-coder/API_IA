# Arquitetura do Sistema — Chat RAG Marvel

## Sumário

1. [Diagrama da Arquitetura](#1-diagrama-da-arquitetura)
2. [Responsabilidades de Cada Módulo](#2-responsabilidades-de-cada-módulo)
3. [Dependências Entre Módulos](#3-dependências-entre-módulos)
4. [Sequência de Chamadas dos Fluxos](#4-sequência-de-chamadas-dos-fluxos)
   - [4.1 Fluxo Completo de uma Mensagem](#41-fluxo-completo-de-uma-mensagem)
   - [4.2 Fluxo RAG](#42-fluxo-rag)
   - [4.3 Fluxo de Upload](#43-fluxo-de-upload)
   - [4.4 Fluxo de OCR](#44-fluxo-de-ocr)
   - [4.5 Fluxo Vision](#45-fluxo-vision)
   - [4.6 Fluxo Streaming (SSE)](#46-fluxo-streaming-sse)
   - [4.7 Fluxo Async](#47-fluxo-async)
   - [4.8 Fluxo do Banco de Dados](#48-fluxo-do-banco-de-dados)
   - [4.9 Fluxo do pgvector](#49-fluxo-do-pgvector)
   - [4.10 Fluxo do Ollama](#410-fluxo-do-ollama)
   - [4.11 Fluxo das Sessões](#411-fluxo-das-sessões)
   - [4.12 Fluxo das Conversas](#412-fluxo-das-conversas)
   - [4.13 Fluxo dos Documentos](#413-fluxo-dos-documentos)
   - [4.14 Fluxo dos Embeddings](#414-fluxo-dos-embeddings)
5. [Pontos Fortes](#5-pontos-fortes)
6. [Problemas Encontrados](#6-problemas-encontrados)
7. [Oportunidades de Melhoria](#7-oportunidades-de-melhoria)

---

## 1. Diagrama da Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE (REST/SSE)                             │
└──────────────────┬──────────────────┬──────────────────┬────────────────────┘
                   │                  │                  │
                   ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          CONTROLLER LAYER                                   │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐ │
│  │ ChatController│ │DocController │ │UploadController│ │SessionController   │ │
│  │ /api/chat     │ │ /api/doc     │ │ /api/upload   │ │ /api/session       │ │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────────┬───────────┘ │
│         │                │                │                   │            │
│         │    ┌───────────┘                │                   │            │
│         ▼    ▼                            ▼                   ▼            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │          CONFIG / EXCEPTION HANDLER / MAPPER / UTIL                 │  │
│  │  CorsConfig, GlobalExceptionHandler, Mappers, FileUtils, etc.       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SERVICE LAYER                                      │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  CHAT DOMAIN                                                         │  │
│  │  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐ │  │
│  │  │ ChatService     │  │ ConversationService│  │ SessionService     │ │  │
│  │  │ (interface)     │  │                    │  │                    │ │  │
│  │  │ ├SimulatedChat  │  │ (histórico,       │  │ (criar, obter,     │ │  │
│  │  │ └RagChatService │  │  conversas)       │  │  invalidar)        │ │  │
│  │  └────────────────┘  └──────────────────┘  └──────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  RAG DOMAIN                                                          │  │
│  │  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐ │  │
│  │  │ EmbeddingService│  │ RetrievalService │  │ PromptBuilder       │ │  │
│  │  │ (interface)     │  │ (interface)      │  │  + MarvelPromptB.  │ │  │
│  │  │ └OllamaEmbedding│  │ ├NoopRetrieval   │  └──────────────────────┘ │  │
│  │  └────────────────┘  │ └VectorRetrieval  │                            │
│  │                       └──────────────────┘                            │
│  │  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐ │  │
│  │  │ OllamaChatServ.│  │ OllamaVisionServ.│  │ ChunkService        │ │  │
│  │  │ (LLM geração)  │  │ (descrição img)  │  │ (texto → chunks)    │ │  │
│  │  └────────────────┘  └──────────────────┘  └──────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  DOCUMENT DOMAIN                                                     │  │
│  │  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────┐  │  │
│  │  │ DocumentIngestServ.│  │ DocumentQueryServ. │  │ UploadService │  │  │
│  │  │ (parse → chunk →   │  │ (CRUD + search)    │  │ (upload file  │  │  │
│  │  │  embed → save)     │  │                    │  │  → attach)    │  │  │
│  │  └────────┬───────────┘  └────────────────────┘  └───────────────┘  │  │
│  │           │                                                          │  │
│  │  ┌────────▼──────────────────────────────────────────────────────┐  │  │
│  │  │  PARSER STRATEGY (ParserFactory + DocumentParser impls)       │  │  │
│  │  │  PdfParser | TxtParser | HtmlParser | MarkdownParser |        │  │  │
│  │  │  WordParser | ImageOcrParser | UrlParser                       │  │  │
│  │  └───────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  MARVEL DOMAIN                                                      │  │
│  │  ┌────────────────┐  ┌──────────────────────┐  ┌──────────────────┐ │  │
│  │  │ MarvelApiService│  │ MarvelFandomIngestion│  │MarvelWikipedia   │ │  │
│  │  │ (API externa)  │  │ (Fandom scraping)    │  │(Wikipedia API)   │ │  │
│  │  └────────────────┘  └──────────────────────┘  └──────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  INFRA DOMAIN                                                       │  │
│  │  ┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐ │  │
│  │  │ FileStorageSer.│  │ WebhookService   │  │ TaskService          │ │  │
│  │  │ (salvar/deletar│  │ (n8n notificação)│  │ (async task mgmt)    │ │  │
│  │  │  arquivos)     │  │ (com retry)      │  │ (em memória)         │ │  │
│  │  └────────────────┘  └──────────────────┘  └──────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          REPOSITORY LAYER (JPA)                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────┐ │
│  │ Session  │ │Conversat │ │ Message  │ │Attachm   │ │Document  │ │ Doc │ │
│  │ Repo     │ │ Repo     │ │ Repo     │ │ Repo     │ │ Repo     │ │Chunk│ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └─────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DATABASE (PostgreSQL 16 + pgvector)                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────────────────┐ │
│  │ sessions    │ │conversations │ │ messages    │ │ attachments          │ │
│  ├─────────────┤ ├─────────────┤ ├─────────────┤ ├──────────────────────┤ │
│  │ documents   │ │document_    │ │ document_   │ │ (vector extension)   │ │
│  │             │ │chunks       │ │ metadata    │ │ IVFFlat index        │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └──────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          INFRAESTRUTURA (Docker)                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ PostgreSQL   │  │ Ollama       │  │ n8n          │  │ Backend (Java) │ │
│  │ pgvector:pg16│  │ 2 modelos    │  │ workflows    │  │ Spring Boot    │ │
│  │ port 5432    │  │ port 11434   │  │ port 5678    │  │ port 8080      │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. Responsabilidades de Cada Módulo

### 2.1 Controller Layer (`com.project.chat.controller`)

| Controller | Responsabilidade |
|---|---|
| `ChatController` | Gerencia mensagens, streaming, async, upload-and-ask, histórico de conversas |
| `DocumentController` | CRUD de documentos, ingestão de arquivos/URLs, busca semântica |
| `UploadController` | Upload simples de arquivos (com validação de sessão e tipo) |
| `SessionController` | Criação, consulta e invalidação de sessões |
| `HealthController` | Health checks (DB, Ollama, disco) e liveness probe |
| `MarvelIngestionController` | Ingestão em massa de fontes Marvel (Fandom, Wikipedia, API oficial) |

### 2.2 Service Layer

#### Chat Domain
| Serviço | Responsabilidade |
|---|---|
| `ChatService` (interface) | Contrato: sendMessage, uploadAndAsk, getHistory, getConversation, sendMessageAsync, sendMessageStream |
| `RagChatService` | Implementação RAG real: embedding + retrieval + Ollama (perfil `rag`) |
| `SimulatedChatService` | Implementação simulada sem dependências externas (perfil `dev`) |
| `ConversationService` | Histórico de conversas e mensagens |
| `SessionService` | Gerenciamento de sessões (criar, obter, invalidar) |
| `PromptBuilder` | Montagem de prompts com contexto RAG (delega para MarvelPromptBuilder) |
| `ChunkService` | Divisão de texto em chunks com overlap e quebra por sentença |

#### RAG Domain
| Serviço | Responsabilidade |
|---|---|
| `EmbeddingService` (interface) | Contrato: embed (texto → vetor), embedAll (batch) |
| `OllamaEmbeddingService` | Geração de embeddings via Ollama (perfil `rag`) |
| `RetrievalService` (interface) | Contrato: search (query/topK e vetor/topK) |
| `VectorRetrievalService` | Busca por similaridade via pgvector (perfil `rag`) |
| `NoopRetrievalService` | Retorna lista vazia (perfil `dev`) |
| `OllamaChatService` | Geração de texto via Ollama (streaming e não-streaming) |
| `OllamaVisionService` | Descrição de imagens via modelo de visão Ollama |

#### Document Domain
| Serviço | Responsabilidade |
|---|---|
| `DocumentIngestionService` | Pipeline completo: parse → chunk → embed → salvar → webhook |
| `DocumentQueryService` | CRUD de documentos, busca semântica com cálculo de similaridade |
| `UploadService` | Upload de arquivos com validação, salvamento e criação de Attachment |
| `FileStorageService` | Armazenamento físico de arquivos no disco |

#### Parser Strategy (`service.parser`)
| Classe | Responsabilidade |
|---|---|
| `DocumentParser` (interface) | Contrato: parse(InputStream) + supports(sourceType) |
| `ParserFactory` | Seleciona o parser correto via `supports()` |
| `PdfParser` | Extrai texto de PDFs via Apache PDFBox |
| `TxtParser` | Lê texto puro |
| `HtmlParser` | Extrai texto de HTML via jsoup (remove scripts, nav, ads) |
| `MarkdownParser` | Remove marcação Markdown, retorna texto limpo |
| `WordParser` | Extrai texto de DOCX via Apache POI |
| `ImageOcrParser` | OCR via Tesseract (tess4j) para imagens |
| `UrlParser` | Baixa e extrai conteúdo de URLs (com validação de segurança) |

#### Marvel Domain (`service.marvel`)
| Serviço | Responsabilidade |
|---|---|
| `MarvelApiService` | Consome Marvel Comics API oficial (personagens, comics, séries) |
| `MarvelFandomIngestionService` | Scrape do marvel.fandom.com (categorias e páginas) |
| `MarvelWikipediaIngestionService` | Consome Wikipedia API para artigos do MCU |

#### Infra Domain
| Serviço | Responsabilidade |
|---|---|
| `WebhookService` | Notifica n8n após ingestão (com retry) |
| `TaskService` | Gerenciamento de tarefas async (em ConcurrentHashMap) |

### 2.3 AI Agent Module (`com.project.chat.ai`)

| Classe | Responsabilidade |
|---|---|
| `AiAgent` (interface) | Contrato para agente de IA Marvel |
| `MarvelAiAgent` | Implementação do agente especializado no MCU |
| `OllamaClient` | HTTP client para Ollama (geração) |
| `MarvelPromptBuilder` | Constrói prompts com conhecimento enciclopédico do MCU (hardcoded) |

### 2.4 Repository Layer

| Repositório | Entidade | Consultas especiais |
|---|---|---|
| `SessionRepository` | Session | findBySessionId, sessões expiradas |
| `ConversationRepository` | Conversation | findBySession com Pageable, count, EntityGraph |
| `MessageRepository` | Message | findByConversation, findTopByOrder |
| `AttachmentRepository` | Attachment | findByMessageId |
| `DocumentRepository` | Document | findByStatus, findBySourceType |
| `DocumentChunkRepository` | DocumentChunk | findSimilarChunks (native query com pgvector `<->`), deleteByDocumentId |
| `DocumentMetadataRepository` | DocumentMetadata | findByDocumentId, deleteByDocumentId |

### 2.5 Config Layer

| Classe | Propriedade Prefixo | Finalidade |
|---|---|---|
| `ChunkingProperties` | `rag.chunking` | Tamanho do chunk e overlap |
| `RagWebhookProperties` | `rag.webhook` | URL, enabled, retry |
| `StorageProperties` | `chat.storage` | Diretório de upload |
| `OllamaConfig` | `rag.ollama` | HttpClient para Ollama |
| `CorsConfig` | — | Liberação CORS global |

## 3. Dependências Entre Módulos

```
Controller
  ├── Service (ChatService, DocumentIngestionService, etc.)
  ├── Repository (indireto via Service)
  ├── Mapper (indireto)
  └── Exception (para lançamento)

Service
  ├── Repository (JPA)
  ├── Service (outros services)
  ├── Parser (Strategy — injetado via ParserFactory)
  ├── External (Ollama HTTP, Marvel API HTTP, n8n webhook)
  └── Mapper (para conversão)

Parser (Strategy Pattern)
  ├── Nenhuma dependência de Service
  └── Bibliotecas externas (PDFBox, jsoup, tess4j, POI)

AI Agent Module
  ├── AiAgentProperties (config)
  ├── OllamaClient (HTTP)
  └── MarvelPromptBuilder (conhecimento hardcoded)

Repository
  └── Entity (JPA)

Entity
  └── Nenhuma (POJOs com annotations JPA + Hibernate)
```

**Dependências externas (runtime):**
- PostgreSQL 16 + pgvector (port 5432)
- Ollama (port 11434): gemma3:4b + nomic-embed-text + llava
- n8n (port 5678): webhooks de ingestão
- Tesseract OCR: instalado no sistema para ImageOcrParser
- Marvel Comics API (gateway.marvel.com): opcional

## 4. Sequência de Chamadas dos Fluxos

### 4.1 Fluxo Completo de uma Mensagem

```
Cliente                    ChatController            RagChatService               Ollama              PostgreSQL
  │                             │                         │                        │                    │
  │ POST /api/chat/message      │                         │                        │                    │
  │ {sessionId, content,        │                         │                        │                    │
  │  conversationId?}           │                         │                        │                    │
  │────────────────────────────>│                         │                        │                    │
  │                             │ sendMessage(request)    │                        │                    │
  │                             │────────────────────────>│                        │                    │
  │                             │                         │                        │                    │
  │                             │                         │ Valida sessão          │                    │
  │                             │                         │─────────────────────────────────────────────>│
  │                             │                         │<─────────────────────────────────────────────│
  │                             │                         │                        │                    │
  │                             │                         │ Se session expirada    │                    │
  │                             │                         │ → SessionConflictException                  │
  │                             │                         │                        │                    │
  │                             │                         │ Atualiza lastActivity  │                    │
  │                             │                         │─────────────────────────────────────────────>│
  │                             │                         │                        │                    │
  │                             │                         │ Se conversationId      │                    │
  │                             │                         │   nulo: cria nova conv │                    │
  │                             │                         │─────────────────────────────────────────────>│
  │                             │                         │<─────────────────────────────────────────────│
  │                             │                         │                        │                    │
  │                             │                         │ Salva msg USER         │                    │
  │                             │                         │─────────────────────────────────────────────>│
  │                             │                         │                        │                    │
  │                             │                         │ [INÍCIO RAG]           │                    │
  │                             │                         │ embed(content)         │                    │
  │                             │                         │───────────────────────>│                    │
  │                             │                         │   POST /api/embed      │                    │
  │                             │                         │<───────────────────────│                    │
  │                             │                         │   float[] vetor        │                    │
  │                             │                         │                        │                    │
  │                             │                         │ search(vetor, topK=5)  │                    │
  │                             │                         │─────────────────────────────────────────────>│
  │                             │                         │   SELECT, ORDER BY     │                    │
  │                             │                         │   embedding <-> vetor  │                    │
  │                             │                         │<─────────────────────────────────────────────│
  │                             │                         │   List<DocumentChunk>  │                    │
  │                             │                         │                        │                    │
  │                             │                         │ buildWithContext()     │                    │
  │                             │                         │   → MarvelPromptBuilder│                    │
  │                             │                         │   → prompt final       │                    │
  │                             │                         │                        │                    │
  │                             │                         │ generate(prompt)       │                    │
  │                             │                         │───────────────────────>│                    │
  │                             │                         │  POST /api/generate    │                    │
  │                             │                         │<───────────────────────│                    │
  │                             │                         │   String resposta      │                    │
  │                             │                         │                        │                    │
  │                             │                         │ [FIM RAG]              │                    │
  │                             │                         │                        │                    │
  │                             │                         │ Salva msg ASSISTANT    │                    │
  │                             │                         │─────────────────────────────────────────────>│
  │                             │                         │                        │                    │
  │                             │<────────────────────────│                        │                    │
  │                             │ ChatResponse             │                        │                    │
  │<────────────────────────────│                         │                        │                    │
```

### 4.2 Fluxo RAG

```
1. Embedding da pergunta:
   RagChatService.sendMessage()
     → EmbeddingService.embed(content)
       → OllamaEmbeddingService.embed(text)
         → POST /api/embed { model: "nomic-embed-text", input: text }
         ← float[768] vector

2. Retrieval semântico:
     → RetrievalService.search(vetor, topK)
       → VectorRetrievalService.search(vetor, topK)
         → FileUtils.toVectorString(vetor) → "[0.1,0.2,...]"
         → DocumentChunkRepository.findSimilarChunks(vectorStr, 5)
           → native SQL: SELECT * FROM document_chunks
                ORDER BY embedding <-> CAST(:vector AS vector)
                LIMIT 5

3. Montagem do prompt:
     → PromptBuilder.buildWithContext(content, chunks)
       → buildChunkContext(chunks) → formata chunks
       → MarvelPromptBuilder.buildPromptWithContext(question, context)
         → System prompt + Marvel knowledge base + chunks context + pergunta

4. Geração da resposta:
     → OllamaChatService.generate(prompt)
       → POST /api/generate { model: "gemma3:4b", prompt, stream: false }
       ← response
```

### 4.3 Fluxo de Upload

```
Cliente                   UploadController            UploadService        FileStorageService    AttachmentRepo     SessionRepo
  │                             │                         │                      │                    │                │
  │ POST /api/upload            │                         │                      │                    │                │
  │ file + sessionId            │                         │                      │                    │                │
  │────────────────────────────>│                         │                      │                    │                │
  │                             │ uploadFile(file, sid)   │                      │                    │                │
  │                             │────────────────────────>│                      │                    │                │
  │                             │                         │ Valida sessão       │                    │                │
  │                             │                         │────────────────────────────────────────────────────────>│
  │                             │                         │<────────────────────────────────────────────────────────│
  │                             │                         │                      │                    │                │
  │                             │                         │ Valida tamanho      │                    │                │
  │                             │                         │  (MAX_FILE_SIZE)    │                    │                │
  │                             │                         │                      │                    │                │
  │                             │                         │ Valida MIME type    │                    │                │
  │                             │                         │  (ALLOWED_MIME_TYPES)│                    │                │
  │                             │                         │                      │                    │                │
  │                             │                         │ store(file)          │                    │                │
  │                             │                         │─────────────────────>│                    │                │
  │                             │                         │  UUID + extensão     │                    │                │
  │                             │                         │<─────────────────────│                    │                │
  │                             │                         │   Path               │                    │                │
  │                             │                         │                      │                    │                │
  │                             │                         │ Cria Attachment      │                    │                │
  │                             │                         │──────────────────────────────────────────>│                │
  │                             │                         │<──────────────────────────────────────────│                │
  │                             │                         │                      │                    │                │
  │                             │<────────────────────────│                      │                    │                │
  │                             │ UploadResponse          │                      │                    │                │
  │<────────────────────────────│                         │                      │                    │                │
```

### 4.4 Fluxo de OCR

```
Cliente                   DocumentController        DocumentIngestionService    ParserFactory     ImageOcrParser    OllamaEmbedding
  │                             │                         │                         │                    │                │
  │ POST /api/documents/ingest  │                         │                         │                    │                │
  │ file (png/jpg/...)          │                         │                         │                    │                │
  │────────────────────────────>│                         │                         │                    │                │
  │                             │ Detecta tipo: "png"    │                         │                    │                │
  │                             │ Valida SUPPORTED_TYPES │                         │                    │                │
  │                             │                         │                         │                    │                │
  │                             │ ingestFromFile(path,   │                         │                    │                │
  │                             │   "png", filename)     │                         │                    │                │
  │                             │────────────────────────>│                         │                    │                │
  │                             │                         │ Cria Document(PENDING) │                    │                │
  │                             │                         │  → DocumentRepository  │                    │                │
  │                             │                         │                         │                    │                │
  │                             │                         │ getParser("png")        │                    │                │
  │                             │                         │───────────────────────>│                    │                │
  │                             │                         │   ImageOcrParser        │                    │                │
  │                             │                         │<───────────────────────│                    │                │
  │                             │                         │                         │                    │                │
  │                             │                         │ parse(inputStream)      │                    │                │
  │                             │                         │─────────────────────────────────────────────>│                │
  │                             │                         │  ImageIO.read()          │                    │                │
  │                             │                         │  Tesseract.doOCR()      │                    │                │
  │                             │                         │<─────────────────────────────────────────────│                │
  │                             │                         │  texto extraído        │                    │                │
  │                             │                         │                         │                    │                │
  │                             │                         │ chunkService.chunkText()│                    │                │
  │                             │                         │ embeddingService.embedAll()                    │                │
  │                             │                         │──────────────────────────────────────────────────>                │
  │                             │                         │<──────────────────────────────────────────────────│                │
  │                             │                         │                         │                    │                │
  │                             │                         │ Salva DocumentChunks    │                    │                │
  │                             │                         │ webhookService.notify() │                    │                │
  │                             │                         │                         │                    │                │
  │                             │<────────────────────────│                         │                    │                │
  │                             │ 202 ACCEPTED            │                         │                    │                │
  │<────────────────────────────│                         │                         │                    │                │
```

**Nota:** O OCR depende do Tesseract instalado no sistema. Se falhar, retorna string vazia silenciosamente.

### 4.5 Fluxo Vision

```
Cliente                   ChatController             RagChatService           FileStorage     ImageOcrParser  OllamaVision
  │                             │                         │                      │                │                │
  │ POST /api/chat/             │                         │                      │                │                │
  │   upload-and-ask            │                         │                      │                │                │
  │ file + sessionId + content  │                         │                      │                │                │
  │────────────────────────────>│                         │                      │                │                │
  │                             │ store(file)             │                      │                │                │
  │                             │──────────────────────────────────────────────>│                │                │
  │                             │<──────────────────────────────────────────────│                │                │
  │                             │                         │                      │                │                │
  │                             │ uploadAndAsk(request)   │                      │                │                │
  │                             │────────────────────────>│                      │                │                │
  │                             │                         │                      │                │                │
  │                             │                         │ Detecta tipo imagem  │                │                │
  │                             │                         │                      │                │                │
  │                             │                         │ Tenta OCR primeiro   │                │                │
  │                             │                         │────────────────────────────────────────────────>│
  │                             │                         │ parse(inputStream)   │                │                │
  │                             │                         │<────────────────────────────────────────────────│
  │                             │                         │   texto OCR          │                │                │
  │                             │                         │                      │                │                │
  │                             │                         │ Se OCR vazio ou nulo:│                │                │
  │                             │                         │ describeImage(bytes)  │                │                │
  │                             │                         │──────────────────────────────────────────────>│
  │                             │                         │   POST /api/generate │                │                │
  │                             │                         │   model: "llava"     │                │                │
  │                             │                         │   images: [base64]   │                │                │
  │                             │                         │<──────────────────────────────────────────────│
  │                             │                         │   descrição textual  │                │                │
  │                             │                         │                      │                │                │
  │                             │                         │ Usa texto como raw   │                │                │
  │                             │                         │   context para RAG   │                │                │
  │                             │                         │                      │                │                │
  │                             │                         │ buildWithRawContext() │                │                │
  │                             │                         │  → OllamaChatService  │                │                │
  │                             │                         │                      │                │                │
  │                             │<────────────────────────│                      │                │                │
  │<────────────────────────────│                         │                      │                │                │
```

### 4.6 Fluxo Streaming (SSE)

```
Cliente                         ChatController          RagChatService           OllamaChatService       Ollama
  │                                   │                       │                        │                   │
  │ POST /api/chat/message/stream     │                       │                        │                   │
  │ Accept: text/event-stream         │                       │                        │                   │
  │──────────────────────────────────>│                       │                        │                   │
  │                                   │ sendMessageStream()   │                        │                   │
  │                                   │──────────────────────>│                        │                   │
  │                                   │                       │                        │                   │
  │                                   │                       │ Cria SseEmitter        │                   │
  │                                   │                       │ (timeout 300s)         │                   │
  │                                   │                       │                        │                   │
  │                                   │                       │ async:                 │                   │
  │                                   │                       │   embed → retrieve     │                   │
  │                                   │                       │   context → prompt     │                   │
  │                                   │                       │                        │                   │
  │                                   │                       │ generateStream(prompt) │                   │
  │                                   │                       │───────────────────────>│                   │
  │                                   │                       │                        │ POST /api/generate│
  │                                   │                       │                        │ stream=true       │
  │                                   │                       │                        │──────────────────>│
  │                                   │                       │                        │                   │
  │ <token>                           │                       │ <──token────────────── │                   │
  │ SSE: event="token", data="..."    │                       │                        │                   │
  │<──────────────────────────────────│                       │                        │                   │
  │                                   │                       │                        │                   │
  │ <token>                           │                       │ <──token────────────── │                   │
  │ SSE: event="token", data="..."    │                       │                        │                   │
  │<──────────────────────────────────│                       │                        │                   │
  │                                   │                       │                        │                   │
  │ ...                               │                       │                        │                   │
  │                                   │                       │                        │                   │
  │ <done>                            │                       │ <──done─────────────── │                   │
  │ SSE: event="done",                │                       │                        │                   │
  │      data={userMessage,           │                       │                        │                   │
  │             assistantMessage,     │                       │                        │                   │
  │             conversationId}       │                       │                        │                   │
  │<──────────────────────────────────│                       │                        │                   │
  │                                   │                       │                        │                   │
```

### 4.7 Fluxo Async

```
Cliente                         ChatController          RagChatService          TaskService
  │                                   │                       │                      │
  │ POST /api/chat/message/async      │                       │                      │
  │──────────────────────────────────>│                       │                      │
  │                                   │ sendMessageAsync()    │                      │
  │                                   │──────────────────────>│                      │
  │                                   │                       │ createTask()          │
  │                                   │                       │─────────────────────>│
  │                                   │                       │<─────────────────────│
  │                                   │                       │   taskId (UUID)      │
  │                                   │<──────────────────────│                      │
  │                                   │ 202 ACCEPTED          │                      │
  │                                   │ {taskId, status:"PENDING"}                   │
  │<──────────────────────────────────│                       │                      │
  │                                   │                       │                      │
  │                                   │                       │ asyncExecutor:       │
  │                                   │                       │   startProcessing()  │
  │                                   │                       │─────────────────────>│
  │                                   │                       │   status=PROCESSING  │
  │                                   │                       │                      │
  │                                   │                       │   sendMessage()      │
  │                                   │                       │     (RAG completo)   │
  │                                   │                       │                      │
  │                                   │                       │   complete() ou fail()│
  │                                   │                       │─────────────────────>│
  │                                   │                       │                      │
  │ (Polling)                         │                       │                      │
  │ GET /api/chat/message/async/{id}  │                       │                      │
  │──────────────────────────────────>│                       │                      │
  │                                   │ getTaskStatus(id)     │                      │
  │                                   │──────────────────────>│                      │
  │                                   │                       │ getTask()            │
  │                                   │                       │─────────────────────>│
  │                                   │                       │<─────────────────────│
  │                                   │<──────────────────────│                      │
  │<──────────────────────────────────│                       │                      │
  │ {taskId, status:"COMPLETED",      │                       │                      │
  │  result: ChatResponse}            │                       │                      │
```

### 4.8 Fluxo do Banco de Dados

```
Modelo Entidade-Relacionamento:

  Session (1) ────────── (N) Conversation (1) ────────── (N) Message (1) ── (0..1) Attachment
    │ sessionId (UUID)        │ title                       │ role (USER/ASSISTANT)
    │ createdAt               │ active                      │ content (TEXT)
    │ lastActivity            │ createdAt                   │ timestamp
    │ expired                 │ updatedAt                   │
                              │                             │
  Document (1) ─────── (N) DocumentChunk
    │ fileName                │ chunkIndex
    │ sourcePath              │ content (TEXT)
    │ sourceType              │ embedding (VECTOR(768))
    │ status (enum)           │ tokenCount
    │ fileSize                │
    │ errorMessage            │
    │ totalChunks             │
    │                         │
    └────── (N) DocumentMetadata (chave-valor)

Fluxo JPA/Hibernate:
  - ddl-auto: update (cria/atualiza tabelas automaticamente)
  - HikariCP pool (5-10 conexões)
  - PostgreSQLDialect + pgvector extension
  - @Transactional gerenciado pelo Spring
  - NamedEntityGraph para eager fetch de mensagens
  - @PrePersist/@PreUpdate para timestamps automáticos
  - @PreRemove em Attachment para deletar arquivo físico
```

### 4.9 Fluxo do pgvector

```
1. Inicialização:
   DatabaseInitializer @PostConstruct
     → CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
         ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)

   Migration V1:
     → CREATE EXTENSION IF NOT EXISTS vector

2. Armazenamento:
   DocumentChunk.embedding (float[]) mapeado via:
     @JdbcTypeCode(SqlTypes.VECTOR)
     @Array(length = 768)
   Hibernate converte float[] para formato vector do PostgreSQL

3. Busca por similaridade (cosseno):
   DocumentChunkRepository.findSimilarChunks(embeddingStr, limit):
     SELECT * FROM document_chunks
     ORDER BY embedding <-> CAST(:embedding AS vector)
     LIMIT :limit

   O operador <-> calcula distância cosseno (1 - cosine_similarity)

4. Utilização:
   VectorRetrievalService.search(float[] vector, int topK)
     → FileUtils.toVectorString() → "[0.1,0.2,...,0.768]"
     → DocumentChunkRepository.findSimilarChunks(string, topK)
```

### 4.10 Fluxo do Ollama

```
Ollama roda em container Docker (ollama/ollama:latest) com 2-3 modelos:

┌─────────────────────────────────────────────────────────────────────┐
│                        OLLAMA (porta 11434)                         │
│                                                                     │
│  Modelos carregados:                                                │
│    • gemma3:4b      → OllamaChatService  (geração de texto)        │
│    • nomic-embed-text → OllamaEmbeddingService (embeddings)         │
│    • llava           → OllamaVisionService (descrição imagens)      │
│                                                                     │
│  Endpoints usados:                                                  │
│    POST /api/generate   → chat/geração (stream + non-stream)       │
│    POST /api/embed      → embeddings (fallback para /api/embeddings)│
│    GET  /api/tags       → health check                              │
│                                                                     │
│  Configuração:                                                      │
│    OLLAMA_KEEP_ALIVE=24h  (modelos ficam carregados 24h)            │
└─────────────────────────────────────────────────────────────────────┘

Fluxo de chamada:
  OllamaConfig.ollamaHttpClient()
    → HttpClient.newBuilder().connectTimeout(10s)

  OllamaChatService.generate(prompt):
    → POST /api/generate
      { model, prompt, stream: false,
        options: { temperature, num_predict } }
    ← { response, done, ... }

  OllamaEmbeddingService.embed(text):
    → POST /api/embed
      { model: "nomic-embed-text", input: text }
    ← { embeddings: [[f1, f2, ..., f768]] }
    Fallback: POST /api/embeddings (modelo antigo)

  OllamaVisionService.describeImage(bytes):
    → POST /api/generate
      { model: "llava", prompt: "Descreva...", images: ["base64..."] }
    ← { response }
```

### 4.11 Fluxo das Sessões

```
Cliente                SessionController      SessionService        SessionRepo
  │                         │                      │                    │
  │ GET /api/session        │                      │                    │
  │────────────────────────>│                      │                    │
  │                         │ createSession()      │                    │
  │                         │─────────────────────>│                    │
  │                         │                      │ Cria Session(UUID) │
  │                         │                      │───────────────────>│
  │                         │                      │<───────────────────│
  │                         │<─────────────────────│                    │
  │<────────────────────────│ sessionId + timestamps│                   │
  │                         │                      │                    │
  │ (Usa sessionId em       │                      │                    │
  │  todas as requisições)  │                      │                    │
  │                         │                      │                    │
  │ GET /api/session/{id}   │                      │                    │
  │────────────────────────>│ getSession(id)       │                    │
  │                         │─────────────────────>│                    │
  │                         │                      │ findBySessionId()  │
  │                         │                      │───────────────────>│
  │                         │                      │<───────────────────│
  │                         │<─────────────────────│                    │
  │<────────────────────────│                      │                    │
  │                         │                      │                    │
  │ DELETE /api/session/{id}│                      │                    │
  │────────────────────────>│ invalidateSession()  │                    │
  │                         │─────────────────────>│ expired=true       │
  │                         │                      │───────────────────>│
  │                         │<─────────────────────│                    │
  │<────────────────────────│ 204 NO CONTENT       │                    │
```

### 4.12 Fluxo das Conversas

```
Criação (primeira mensagem da sessão):
  1. ChatRequest sem conversationId
  2. RagChatService cria Conversation(session, title)
     title = primeiros 50 chars da mensagem + "..."
  3. Salva no banco
  4. Retorna conversationId no ChatResponse

Reuso (mensagens seguintes):
  1. ChatRequest com conversationId
  2. Verifica se conversationId pertence à sessionId
  3. Adiciona mensagem à conversa existente

Histórico:
  1. GET /api/chat/history/{sessionId}
     → ConversationService.getHistory()
     → findConversationsBySession (paginado, max 100)
     → Retorna summaries com última mensagem

  2. GET /api/chat/history/{sessionId}/{conversationId}
     → ConversationService.getConversation()
     → Verifica ownership session-conversation
     → Retorna todas as mensagens ordenadas por timestamp
```

### 4.13 Fluxo dos Documentos

```
Pipeline de Ingestão (DocumentIngestionService):

  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌───────────┐    ┌──────────┐
  │  FILE   │    │  PARSE  │    │  CHUNK  │    │  EMBED    │    │  SAVE    │
  │  UPLOAD │───>│  (Text) │───>│ (Split) │───>│ (Vector)  │───>│ (DB +    │
  │  Storage│    │         │    │         │    │           │    │ Webhook) │
  └─────────┘    └─────────┘    └─────────┘    └───────────┘    └──────────┘
       │              │              │               │               │
       ▼              ▼              ▼               ▼               ▼
  ./uploads/    ParserFactory   ChunkService   OllamaEmbedding  DocumentRepo
  UUID.ext      .getParser()    .chunkText()   .embedAll()      + DocumentChunkRepo
                                                              + WebhookService

Status lifecycle:
  PENDING → PROCESSING → COMPLETED
                       → FAILED (com errorMessage)

CRUD (DocumentQueryService):
  - listDocuments() → findAll (sem paginação)
  - getDocument(id) → findById
  - deleteDocument(id) → deleta chunks + documento + arquivo físico
  - getDocumentChunks(id) → findByDocumentId (ordenado)
  - searchSimilar(query, topK) → embed → findSimilarChunks → calcula similaridade cosseno
```

### 4.14 Fluxo dos Embeddings

```
Geração:
  OllamaEmbeddingService.embed(text)
    1. Trunca para MAX_CHARS (8000) se necessário
    2. POST /api/embed { model, input }
    3. Fallback: POST /api/embeddings { model, prompt }
    4. Extrai embeddings[0] do response
    5. Converte List<Number> → float[768]

Batch:
  OllamaEmbeddingService.embedAll(texts)
    1. Divide em lotes de MAX_PARALLEL (2)
    2. parallelStream() para chamadas concorrentes
    3. Retorna List<float[]>

Armazenamento:
  DocumentChunk.embedding (float[]) via Hibernate
    @JdbcTypeCode(SqlTypes.VECTOR)
    → PostgreSQL column type: vector(768)

Recuperação:
  VectorRetrievalService.search(float[] vector, int topK)
    1. FileUtils.toVectorString() → "[0.1,0.2,...]"
    2. SQL nativo ORDER BY embedding <-> CAST(:v AS vector)
```

## 5. Pontos Fortes

1. **Clean Architecture claro**: Separação nítida entre controller, service, repository, entity, mapper, exception. Controllers não têm regra de negócio.

2. **Strategy Pattern bem aplicado**: `DocumentParser` com `ParserFactory` permite adicionar novos parsers sem modificar código existente (aberto para extensão, fechado para modificação).

3. **Interface-based design**: `ChatService`, `EmbeddingService`, `RetrievalService` são interfaces com múltiplas implementações selecionáveis por perfil Spring (`dev` vs `rag`).

4. **Perfis Spring bem utilizados**: `dev` (simulação) vs `rag` (real) vs `prod` (produção) com configurações específicas.

5. **RAG completo**: Pipeline ponta a ponta documentado e funcional: ingestão → parse → chunk → embed → store → retrieve → generate.

6. **Segurança em URL parser**: Bloqueia endereços internos (localhost, 10.x, 172.16.x, 192.168.x, .local, .internal) e valida scheme HTTP/HTTPS.

7. **Path traversal prevention**: `FileStorageService` valida que o path resolvido está dentro do diretório de upload.

8. **Exception hierarchy robusta**: 11 exceções específicas + `GlobalExceptionHandler` com mapeamento HTTP adequado.

9. **Webhook com retry**: `WebhookService` possui mecanismo de retry configurável (3 tentativas com delay) para notificar n8n.

10. **Multimodalidade**: Suporte a PDF, TXT, Markdown, HTML, DOCX, URLs, imagens (OCR + Vision).

11. **pgvector nativo**: Embeddings armazenados em coluna VECTOR nativa do PostgreSQL, com índice IVFFlat para busca eficiente.

12. **Streaming SSE**: Respostas do Ollama em tempo real via Server-Sent Events.

## 6. Problemas Encontrados

1. **RagChatService é God Object**: Depende de 15+ beans, acumula responsabilidades de chat, RAG, visão, OCR, upload, streaming, async, histórico. Viola SRP severamente.

2. **MarvelIngestionController contém regra de negócio**: Injeção direta de repositórios e serviços de embedding/chunk, realizando todo o pipeline de ingestão Marvel dentro do controller.

3. **Duplicação de pipeline de ingestão**: `MarvelIngestionController.ingestFromMarvelApi()` replica a lógica de chunking + embedding que já existe em `DocumentIngestionService.processDocument()`.

4. **Duplicação entre Fandom/Wikipedia**: `MarvelFandomIngestionService` e `MarvelWikipediaIngestionService` têm ~80% de código duplicado (chunk → embed → save → webhook).

5. **Duplicação de sessão/conversação**: `SimulatedChatService` e `RagChatService` compartilham lógica idêntica de validação de sessão, criação de conversa e salvamento de mensagem.

6. **TaskService em memória**: `ConcurrentHashMap` para tasks async — tasks são perdidas em restart.

7. **Executors não gerenciados**: `RagChatService` cria `Executors.newFixedThreadPool(10)` que nunca é shutdown.

8. **Sync webhook na request**: `WebhookService.notify()` é chamado síncronamente dentro do pipeline de ingestão, bloqueando a resposta.

9. **Sem paginação em listDocuments**: `DocumentRepository.findAll()` sem `Pageable` pode causar problemas com muitos documentos.

10. **MAX_CHARS hardcoded**: `OllamaEmbeddingService` usa 8000 caracteres como limite máximo para embedding sem configuração externa.

11. **Sem sessão de expiração**: `SessionRepository.findByExpiredFalseAndLastActivityBefore()` existe mas não há scheduler que chame este método.

12. **Dead code**: `PdfTextExtractor` está `@Deprecated` e nunca é usado, mas mantido como legacy.

13. **Inconsistência no índice IVFFlat**: `DatabaseInitializer` cria índice com nome `idx_document_chunks_embedding`, mas migration V1 também cria um índice (nomes diferentes ou duplicados).

14. **Configuração duplicada**: `application-rag.yml` e `application-prod.yml` têm grande sobreposição de configuração.

15. **AI Agent Module não integrado**: `MarvelAiAgent` existe como módulo separado mas o fluxo RAG real usa `OllamaChatService` + `MarvelPromptBuilder` diretamente, ignorando o agente.

16. **Sem rate limit no Ollama**: `OllamaEmbeddingService.embedAll()` usa `parallelStream()` com `MAX_PARALLEL=2`, mas não há controle de rate limit global.

## 7. Oportunidades de Melhoria

1. **Extrair SessionValidator**: Criar um service separado para validação de sessão usado por RagChatService e SimulatedChatService.

2. **Extrair ConversationManager**: Service para criação/reuso de conversas, removendo duplicação entre implementações de ChatService.

3. **Unificar pipeline de ingestão**: Fazer MarvelFandomIngestionService e MarvelWikipediaIngestionService usarem DocumentIngestionService.processDocument() em vez de duplicar.

4. **Mover MarvelIngestionController para Service**: A lógica de ingestão Marvel deve estar em services dedicados, não no controller.

5. **Adicionar TaskRepository**: Persistir tasks async no banco de dados em vez de ConcurrentHashMap.

6. **Gerenciar ciclo de vida dos Executors**: Usar @PreDestroy para shutdown, ou melhor, usar ThreadPoolTaskExecutor do Spring.

7. **Webhook assíncrono**: Usar @Async ou fila (RabbitMQ/SQS) para notificações n8n sem bloquear a resposta.

8. **Adicionar paginação**: DocumentRepository.findAll() deve receber Pageable.

9. **Externalizar MAX_CHARS**: Mover para application.yml como `rag.ollama.max-embedding-chars`.

10. **Scheduler de expiração**: @Scheduled para limpar sessões expiradas periodicamente.

11. **Rate limiter para Ollama**: Implementar semáforo ou fila para controlar concorrência nas chamadas de embedding.

12. **Integrar AI Agent Module**: Unificar MarvelAiAgent com o fluxo RAG real, removendo duplicação de OllamaClient.

13. **Testes de integração**: Adicionar testes que validem o pipeline completo (controller → service → repository → DB).

14. **Documentação da API com OpenAPI**: Adicionar springdoc-openapi para geração automática de Swagger.

# API Backend — Marvel RAG Chat — Especificação Completa para Frontend

> Documento gerado automaticamente após análise de 100% do código-fonte.
> Versão: 2.0.0 | Stack: Java 17 + Spring Boot 3.4.5 + PostgreSQL 16 + pgvector + Ollama

---

## Sumário

1. [Stack Tecnológica](#1-stack-tecnológica)
2. [Arquitetura Geral](#2-arquitetura-geral)
3. [Perfis de Ambiente](#3-perfis-de-ambiente)
4. [Endpoints da API](#4-endpoints-da-api)
   - 4.1 [Sessão](#41-sessão)
   - 4.2 [Chat](#42-chat)
   - 4.3 [Upload de Anexos](#43-upload-de-anexos)
   - 4.4 [Documentos (Ingestão RAG)](#44-documentos-ingestão-rag)
   - 4.5 [Busca Semântica](#45-busca-semântica)
   - 4.6 [Health Check](#46-health-check)
5. [Fluxo do Chat RAG](#5-fluxo-do-chat-rag)
6. [Fluxo de Ingestão de Documentos](#6-fluxo-de-ingestão-de-documentos)
7. [Modelo de Dados (Entidades JPA)](#7-modelo-de-dados-entidades-jpa)
8. [Mapeamento DTO → Entidade](#8-mapeamento-dto--entidade)
9. [Tratamento de Erros](#9-tratamento-de-erros)
10. [Códigos de Status HTTP](#10-códigos-de-status-http)
11. [Regras de Negócio Importantes](#11-regras-de-negócio-importantes)
12. [Cabeçalhos CORS](#12-cabeçalhos-cors)
13. [Testes](#13-testes)
14. [Docker](#14-docker)

---

## 1. Stack Tecnológica

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 17+ |
| Framework | Spring Boot 3.4.5 |
| ORM | Hibernate 6 + Spring Data JPA |
| Banco | PostgreSQL 16 + pgvector 0.8.3 |
| Vector Search | pgvector (distância cosseno `<->`) |
| LLM Local | Ollama — modelo `gemma4` (8B Q4_K_M) |
| Embedding | Ollama — modelo `nomic-embed-text` (768 dims) |
| Parsing PDF | Apache PDFBox 3.0.4 |
| Parsing HTML | Jsoup 1.18.3 |
| Webhook | n8n (opcional) |
| Build | Maven 3.9+ |
| Container | Docker + Docker Compose |

## 2. Arquitetura Geral

```
Frontend (React/Vue/etc)
       │
       │ HTTP (JSON)
       ▼
 Spring Boot API (porta 8080)
       │
       ├─► PostgreSQL (porta 5432) — Dados persistentes + vetores
       │
       ├─► Ollama (porta 11434) — LLM + Embeddings locais
       │      ├─ gemma4 (chat)
       │      └─ nomic-embed-text (embedding)
       │
       └─► n8n (porta 5678) — Webhook opcional pós-ingestão
```

**Estrutura de Pacotes:**
```
com.project.chat
├── controller        # REST controllers (delegam para services)
├── service           # Interfaces + implementações + Strategy parsers
│   └── parser        # DocumentParser strategy pattern
├── repository        # JPA + Native SQL (pgvector)
├── entity            # JPA entities + enums
├── dto
│   ├── request/      # Input DTOs com validação
│   └── response/     # Output DTOs
├── config            # Beans, CORS, properties
├── exception         # Exceptions + GlobalExceptionHandler
├── mapper            # Entity ↔ DTO
└── util              # FileUtils, PdfTextExtractor
```

## 3. Perfis de Ambiente

| Profile | Ativação | Uso |
|---------|----------|-----|
| `rag` | **default** (`spring.profiles.active: rag`) | Produção real com RAG + Ollama |
| `dev` | Manual | Chat simulado (sem Ollama), embeddings NOOP |
| `prod` | Via Docker/ambiente | Produção conteinerizada |

**Diferenças entre perfis:**
- **rag**: Usa `RagChatService`, `VectorRetrievalService`, `OllamaEmbeddingService`, `OllamaChatService` — pipeline RAG completo
- **dev**: Usa `SimulatedChatService`, `NoopRetrievalService` — respostas mockadas, sem necessidade de Ollama

**Database por perfil:**
- `rag`/`prod`: PostgreSQL (`jdbc:postgresql://localhost:5432/chatdb`)
- `dev`: também PostgreSQL (pode ser alterado para H2)

## 4. Endpoints da API

### 4.1 Sessão

Base URL: `/api/session`

#### `GET /api/session`
Cria uma nova sessão de chat.

**Request body:** Nenhum

**Response `200 OK`:**
```json
{
  "sessionId": "6ac195a2-753a-473d-aec6-0d84b61fc6bb",
  "createdAt": "2026-06-28T19:14:25",
  "lastActivity": "2026-06-28T19:14:25",
  "expired": false
}
```

**Regras:**
- `sessionId` é um UUID v4 gerado pelo backend
- Toda interação (chat, upload) precisa de um `sessionId` válido
- Sessões expiram após 24h de inatividade (configurável via `chat.session.expiration-hours`)
- Sessão expirada retorna `409 Conflict`

#### `DELETE /api/session/{sessionId}`
Invalida uma sessão (marca como expirada).

**Response:** `204 No Content` | `404 Not Found`

---

### 4.2 Chat

Base URL: `/api/chat`

#### `POST /api/chat/message`
Envia uma mensagem e recebe resposta da IA.

**Request body:**
```json
{
  "sessionId": "6ac195a2-753a-473d-aec6-0d84b61fc6bb",
  "conversationId": null,
  "content": "Quem é o Homem de Ferro?",
  "attachmentId": null
}
```

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `sessionId` | string (UUID) | **Sim** | UUID v4 formatado (`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`) |
| `conversationId` | Long | Não | ID da conversa existente. `null` = nova conversa |
| `content` | string | **Sim** | Texto da mensagem (max 5000 caracteres) |
| `attachmentId` | Long | Não | ID de anexo enviado via `/api/upload` |

**Response `200 OK`:**
```json
{
  "userMessage": {
    "id": 61,
    "conversationId": 56,
    "role": "USER",
    "content": "Quem é o Homem de Ferro?",
    "timestamp": "2026-06-28T19:14:29",
    "attachment": null
  },
  "assistantMessage": {
    "id": 62,
    "conversationId": 56,
    "role": "ASSISTANT",
    "content": "O Homem de Ferro é...",
    "timestamp": "2026-06-28T19:14:55",
    "attachment": null
  },
  "conversationId": 56
}
```

**Comportamento:**
- Se `conversationId` é `null`: cria nova conversa (título = primeiros 50 chars da mensagem)
- Se `conversationId` é fornecido: continua conversa existente (valida se pertence à sessão)
- O fluxo RAG: **embedding** → **busca vetorial** → **montagem do prompt** → **LLM** → **resposta**
- A resposta pode levar de 5 a 60 segundos (depende do Ollama)
- `attachment` só aparece se foi enviado anexo para aquela mensagem

#### `GET /api/chat/history/{sessionId}`
Lista todas as conversas de uma sessão (ordenadas por última atividade, decrescente).

**Response `200 OK`:**
```json
{
  "sessionId": "6ac195a2-753a-473d-aec6-0d84b61fc6bb",
  "conversations": [
    {
      "id": 56,
      "title": "Quem é o Homem de Ferro?",
      "messageCount": 2,
      "lastMessage": "O Homem de Ferro é...",
      "lastActivity": "2026-06-28T19:14:55"
    }
  ]
}
```

**Regras:**
- Limite máximo de 50 conversas retornadas (ordenadas por `updatedAt` DESC)
- `lastMessage` = conteúdo da última mensagem (role ASSISTANT)

#### `GET /api/chat/history/{sessionId}/{conversationId}`
Retorna todas as mensagens de uma conversa específica.

**Response `200 OK`:**
```json
{
  "id": 56,
  "messages": [
    {
      "id": 61,
      "conversationId": 56,
      "role": "USER",
      "content": "Quem é o Homem de Ferro?",
      "timestamp": "2026-06-28T19:14:29",
      "attachment": null
    },
    {
      "id": 62,
      "conversationId": 56,
      "role": "ASSISTANT",
      "content": "O Homem de Ferro é...",
      "timestamp": "2026-06-28T19:14:55",
      "attachment": null
    }
  ]
}
```

---

### 4.3 Upload de Anexos

#### `POST /api/upload`
Faz upload de arquivo para vincular a uma mensagem futura.

**Request:** `multipart/form-data`

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `file` | File | **Sim** | Arquivo (.txt ou .pdf, max 10MB) |
| `sessionId` | string | **Sim** | UUID da sessão |

**Response `200 OK`:**
```json
{
  "attachmentId": 1,
  "fileName": "documento.pdf",
  "fileType": "application/pdf",
  "fileSize": 1024000,
  "uploadedAt": "2026-06-28T19:30:00",
  "message": "Arquivo enviado com sucesso."
}
```

**Regras:**
- Tipos permitidos: `text/plain`, `application/pdf`, `image/jpeg`, `image/png`, `image/gif`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- Tamanho máximo: 10 MB
- O arquivo é salvo em disco (`./uploads/`) com nome UUID
- O `attachmentId` retornado deve ser enviado no próximo `POST /api/chat/message` para vincular à mensagem

---

### 4.4 Documentos (Ingestão RAG)

Base URL: `/api/documents`

#### `GET /api/documents`
Lista todos os documentos ingeridos.

**Response `200 OK`:**
```json
{
  "documents": [
    {
      "id": 1,
      "fileName": "guia-mcu.pdf",
      "sourceType": "pdf",
      "fileSize": 2048000,
      "status": "COMPLETED",
      "totalChunks": 42,
      "createdAt": "2026-06-28T19:00:00"
    }
  ]
}
```

**Status possíveis:** `PENDING` | `PROCESSING` | `COMPLETED` | `FAILED`

#### `GET /api/documents/{id}`
Retorna detalhes de um documento específico.

#### `DELETE /api/documents/{documentId}`
Remove documento, seus chunks, metadados e arquivo físico.

**Response:** `204 No Content`

#### `GET /api/documents/{documentId}/chunks`
Retorna todos os chunks de um documento.

**Response `200 OK`:**
```json
[
  {
    "chunkId": 1,
    "documentId": 1,
    "fileName": "guia-mcu.pdf",
    "chunkIndex": 0,
    "content": "Trecho do documento...",
    "similarityScore": null
  }
]
```

#### `POST /api/documents/ingest`
Ingere um arquivo (PDF, TXT, MD, HTML) para a base de conhecimento RAG.

**Request:** `multipart/form-data`

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `file` | File | **Sim** | Arquivo (max 50MB) |
| `sourceType` | string | Não | `pdf`, `txt`, `markdown`, `md`, `html`, `htm` (detectado da extensão se omitido) |

**Response `202 Accepted`:**
```json
{
  "documentId": 1,
  "fileName": "guia-mcu.pdf",
  "status": "PROCESSING",
  "chunks": 0,
  "processingTime": 0,
  "message": "Documento enviado para processamento."
}
```

**Processo assíncrono:**
1. Arquivo é salvo em disco
2. Texto é extraído (PDF via PDFBox, HTML via Jsoup, etc.)
3. Texto é chunked (tamanho 800 chars, overlap 120)
4. Cada chunk recebe embedding (nomic-embed-text, 768 dims)
5. Chunks + embeddings são salvos no PostgreSQL (pgvector)
6. Webhook n8n é disparado (se habilitado)

**Formatos suportados para ingestão e seus parsers:**

| Formato | Extensões | Parser | Biblioteca |
|---------|-----------|--------|------------|
| PDF | `.pdf` | `PdfParser` | Apache PDFBox |
| Texto | `.txt` | `TxtParser` | Nativa (readAllBytes) |
| Markdown | `.md`, `.markdown` | `MarkdownParser` | Regex stripping |
| HTML | `.html`, `.htm` | `HtmlParser` | Jsoup |

#### `POST /api/documents/ingest/url`
Ingere conteúdo de uma URL pública.

**Request body:**
```json
{
  "url": "https://pt.wikipedia.org/wiki/Homem_de_Ferro"
}
```

**Regras:**
- Apenas HTTP/HTTPS
- Bloqueia URLs de rede interna (localhost, 10.x, 172.16.x, 192.168.x, .local, .internal)
- Extrai título e conteúdo textual (remove scripts, nav, footer, etc.)

**Response:** `202 Accepted` (mesmo formato de `/ingest`)

#### `POST /api/documents/search`
Busca semântica nos documentos ingeridos.

**Request body:**
```json
{
  "query": "Quem é o Homem de Ferro?",
  "topK": 5
}
```

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| `query` | string | **Sim** | Texto da consulta |
| `topK` | int | Não | Número de resultados (1-100, default 5) |

**Response `200 OK`:**
```json
{
  "results": [
    {
      "chunkId": 1,
      "documentId": 1,
      "fileName": "guia-mcu.pdf",
      "chunkIndex": 0,
      "content": "Trecho relevante...",
      "similarityScore": 0.87
    }
  ]
}
```

**Como funciona:**
1. A query é convertida em embedding via `/api/embed` (nomic-embed-text)
2. É feita busca por similaridade cosseno no pgvector (`embedding <-> CAST(:vector AS vector)`)
3. O score de similaridade é calculado via **cosseno** (não distância): `dotProduct / (normA * normB)`
4. Valores próximos de 1.0 = maior similaridade

---

### 4.5 Busca Semântica (Endpoint Avulso)

O mesmo `POST /api/documents/search` acima serve como busca semântica independente, fora do fluxo do chat.

---

### 4.6 Health Check

#### `GET /api/health`
Verifica status do backend.

**Response `200 OK`:**
```json
{
  "status": "UP",
  "database": "UP",
  "ollama": "UP",
  "diskSpace": "OK (50 GB disponível)",
  "timestamp": "2026-06-28T19:30:00",
  "version": "2.0.0"
}
```

| Campo | Descrição |
|-------|-----------|
| `status` | `UP` (tudo ok), `DEGRADED` (banco UP mas Ollama DOWN), `DOWN` (banco DOWN) |
| `database` | Testa conexão PostgreSQL (`conn.isValid(2)`) |
| `ollama` | Testa `GET /api/tags` no Ollama (timeout 3s) |
| `diskSpace` | Espaço livre em disco no diretório raiz |

---

## 5. Fluxo do Chat RAG

```
Frontend                          Backend                              Ollama
   │                                 │                                    │
   │─ POST /api/chat/message ───────►│                                    │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ Validações:  │                             │
   │                          │ • sessionId  │                             │
   │                          │ • não expir. │                             │
   │                          │ • content    │                             │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ Salva msg   │                             │
   │                          │ USER no DB  │                             │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ embed(content)─ ─ ─ ─ ─ ─ ─ ─► /api/embed │
   │                          │              ◄─── float[768] ── ─ ─ ─ ─  │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ search(     │                             │
   │                          │  vector,5)  │                             │
   │                          │ ─► SQL:     │                             │
   │                          │ embedding   │                             │
   │                          │ <-> CAST(   │                             │
   │                          │   :vector   │                             │
   │                          │ AS vector)  │                             │
   │                          │ ─► List<    │                             │
   │                          │   Chunk>    │                             │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ Monta prompt│                             │
   │                          │ System:     │                             │
   │                          │ "Você é     │                             │
   │                          │  especial.  │                             │
   │                          │  MCU..."    │                             │
   │                          │ Contexto:   │                             │
   │                          │ chunks      │                             │
   │                          │ Pergunta:   │                             │
   │                          │ content     │                             │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ generate(   │                             │
   │                          │  prompt)─ ─ ─ ─ ─ ─ ► /api/generate       │
   │                          │              ◄─── resposta ── ─ ─ ─ ─     │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │                          ┌──────┴──────┐                             │
   │                          │ Salva msg   │                             │
   │                          │ ASSISTANT   │                             │
   │                          │ no DB       │                             │
   │                          └──────┬──────┘                             │
   │                                 │                                    │
   │◄─── JSON ChatResponse ──────────┘                                    │
```

**Prompt enviado ao Ollama (gemma4):**
```
Você é um especialista absoluto no Universo Cinematográfico Marvel (MCU).
Responda apenas sobre o Universo Marvel...

REGRAS:
1. Responda SOMENTE sobre o MCU
2. Se não for sobre Marvel: "Infelizmente esse conteúdo..."
3. Baseie-se no contexto fornecido
4. Seja detalhista, cite filmes, atores e eventos
5. Responda em português do Brasil

CONHECIMENTO BASE:
O Universo Cinematográfico Marvel (MCU)...

CONTEXTO EXTRAÍDO DOS DOCUMENTOS:
- Trecho do documento relevante...

PERGUNTA DO USUÁRIO: Quem é o Homem de Ferro?
```

## 6. Fluxo de Ingestão de Documentos

```
Frontend                                    Backend
   │                                           │
   │─ POST /api/documents/ingest ─────────────►│
   │  (multipart: file + sourceType)           │
   │                                           │
   │                                    ┌──────┴──────┐
   │                                    │ Valida:      │
   │                                    │ • tamanho    │
   │                                    │ • tipo       │
   │                                    └──────┬──────┘
   │                                           │
   │                                    ┌──────┴──────┐
   │                                    │ Salva em     │
   │                                    │ disco (UUID) │
   │                                    └──────┬──────┘
   │                                           │
   │                                    ┌──────┴──────┐
   │◄─── 202 ACCEPTED ──────────────────│ Cria doc DB  │
   │      {documentId, status:PENDING}  │ PENDING      │
   │                                    └──────┬──────┘
   │                                           │
   │                           ─ ─ ─ ─ ─ ─ ─ ─┘
   │                           Processamento síncrono:
   │                                    │
   │                                    ├─ ParserFactory.getParser(type)
   │                                    │  ├─ PdfParser (PDFBox)
   │                                    │  ├─ TxtParser (readAllBytes)
   │                                    │  ├─ MarkdownParser (regex)
   │                                    │  └─ HtmlParser (Jsoup)
   │                                    │
   │                                    ├─ chunkText() → N chunks
   │                                    │  (tamanho=800, overlap=120)
   │                                    │
   │                                    ├─ embedAll(chunks) → float[N][768]
   │                                    │  └─ POST /api/embed (batelada)
   │                                    │
   │                                    ├─ Salva chunks + embeddings no DB
   │                                    │
   │                                    ├─ Status → COMPLETED
   │                                    │
   │                                    └─ Webhook n8n (se habilitado)
   │                                           │
   │                                           └─ POST http://n8n:5678/webhook/...
   │                                              {documentId, fileName,
   │                                               status, chunks, ...}
```

## 7. Modelo de Dados (Entidades JPA)

### 7.1 Session
```sql
sessions (
  id            BIGSERIAL PRIMARY KEY,
  session_id    VARCHAR(36) NOT NULL UNIQUE,   -- UUID
  created_at    TIMESTAMP NOT NULL,
  last_activity TIMESTAMP NOT NULL,
  expired       BOOLEAN NOT NULL DEFAULT FALSE
)
```
- `session_id` é o UUID público usado nas requisições da API
- `id` é o PK interno usado nos relacionamentos
- Expira após 24h de inatividade (job `@Scheduled` a cada 1h)

### 7.2 Conversation
```sql
conversations (
  id         BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES sessions(id),
  title      VARCHAR(255) NOT NULL,            -- primeiros 50 chars da 1ª msg
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  active     BOOLEAN NOT NULL DEFAULT TRUE
)
```
- Relacionamento: Session 1→N Conversation

### 7.3 Message
```sql
messages (
  id              BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT NOT NULL REFERENCES conversations(id),
  role            VARCHAR(16) NOT NULL,         -- 'USER' | 'ASSISTANT'
  content         TEXT NOT NULL,
  timestamp       TIMESTAMP NOT NULL
)
```
- Relacionamento: Conversation 1→N Message
- Ordenação: `timestamp ASC`

### 7.4 Attachment
```sql
attachments (
  id          BIGSERIAL PRIMARY KEY,
  message_id  BIGINT REFERENCES messages(id),  -- NULL antes de vincular
  file_name   VARCHAR(255) NOT NULL,
  file_type   VARCHAR(127) NOT NULL,            -- MIME type
  file_size   BIGINT NOT NULL,
  storage_path VARCHAR(512) NOT NULL,           -- path no disco
  uploaded_at TIMESTAMP NOT NULL
)
```
- `message_id` é `null` até que o attachment seja vinculado a uma mensagem
- Ao deletar a mensagem, o attachment é removido em cascata (+ arquivo físico)

### 7.5 Document (Conhecimento RAG)
```sql
documents (
  id           BIGSERIAL PRIMARY KEY,
  title        VARCHAR(255) NOT NULL,           -- fileName
  source_url   TEXT,                            -- sourcePath (path no disco ou URL)
  source_type  VARCHAR(31) NOT NULL,            -- 'pdf','txt','markdown','html','url'
  status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING|PROCESSING|COMPLETED|FAILED
  file_size    BIGINT,
  error_message TEXT,
  chunk_count  INT NOT NULL DEFAULT 0,
  created_at   TIMESTAMP NOT NULL,
  updated_at   TIMESTAMP
)
```

### 7.6 DocumentChunk (Vetores)
```sql
document_chunks (
  id           BIGSERIAL PRIMARY KEY,
  document_id  BIGINT NOT NULL REFERENCES documents(id),
  chunk_index  INT NOT NULL,
  content      TEXT NOT NULL,
  embedding    vector(768),                     -- pgvector, 768 dimensões
  token_count  INT NOT NULL,
  created_at   TIMESTAMP NOT NULL
)
```
- `embedding` é coluna VECTOR(768) do pgvector
- Index: `ivfflat` com `vector_cosine_ops` (100 lists)
- Busca: `ORDER BY embedding <-> CAST(:vector AS vector) LIMIT :limit`
- Operador `<->` = distância cosseno

### 7.7 DocumentMetadata (Par chave-valor)
```sql
document_metadata (
  id          BIGSERIAL PRIMARY KEY,
  document_id BIGINT NOT NULL REFERENCES documents(id),
  key         VARCHAR(255) NOT NULL,
  value       TEXT NOT NULL
)
```

**Diagrama de Relacionamentos:**
```
Session ──1:N──► Conversation ──1:N──► Message ──1:1──► Attachment
                                                        (opcional)

Document ──1:N──► DocumentChunk (vector(768))
Document ──1:N──► DocumentMetadata (chave-valor)
```

## 8. Mapeamento DTO → Entidade

### MessageMapper
- `toResponse(Message)` → `MessageResponse`: carrega attachment via `AttachmentRepository.findByMessageId()`
- `toEntity(ChatRequest, Conversation, MessageRole)` → `Message`: mapeia campos + timestamps

### ConversationMapper
- `toSummary(Conversation)` → `ConversationSummaryResponse`: busca última mensagem via `MessageRepository.findTopByConversationIdOrderByTimestampDesc()`
- `toResponse(Conversation, List<Message>)` → `ConversationResponse`: mensagens ordenadas por timestamp ASC

### DocumentMapper
- `toDocumentResponse(Document)` → `DocumentResponse`
- `toChunkResponse(DocumentChunk)` → `DocumentChunkResponse`
- `toIngestionResponse(Document, String)` → `IngestionResponse`

### AttachmentMapper
- `toUploadResponse(Attachment)` → `UploadResponse`

## 9. Tratamento de Erros

Todos os erros são capturados pelo `GlobalExceptionHandler` (`@RestControllerAdvice`) e retornam no formato padronizado:

```json
{
  "status": 502,
  "error": "Bad Gateway",
  "message": "O serviço de inteligência artificial está temporariamente indisponível.",
  "timestamp": "2026-06-28T19:14:29",
  "path": "/api/chat/message",
  "errors": null
}
```

### Mapa de Exceções → HTTP Status

| Exceção | HTTP Status | `error` | Quando ocorre |
|---------|-------------|---------|---------------|
| `ResourceNotFoundException` | 404 | "Not Found" | Sessão, documento, conversa não encontrados |
| `ValidationException` | 422 | "Unprocessable Entity" | Validação manual de negócio |
| `MethodArgumentNotValidException` | 422 | "Unprocessable Entity" | Validação de DTO (@NotBlank, @Pattern, @Size) |
| `UnsupportedFileTypeException` | 415 | "Unsupported Media Type" | Upload de tipo de arquivo não permitido |
| `FileTooLargeException` | 413 | "Payload Too Large" | Arquivo excede 10MB (upload) ou 50MB (ingestão) |
| `MaxUploadSizeExceededException` | 413 | "Payload Too Large" | Excede limite do Spring (11MB) |
| `SessionConflictException` | 409 | "Conflict" | Sessão expirada |
| `EmbeddingException` | 502 | "Bad Gateway" | Falha ao gerar embedding no Ollama |
| `LlmServiceException` | 502 | "Bad Gateway" | Falha no Ollama (chat ou LLM) |
| `WebhookException` | 500 | "Internal Server Error" | Falha no webhook n8n |
| `IngestionException` | 422 | "Unprocessable Entity" | Falha na ingestão de documento |
| `FileCorruptedException` | 400 | "Bad Request" | PDF corrompido |
| `IllegalArgumentException` | 400 | "Bad Request" | Argumento inválido |
| `NoResourceFoundException` | 404 | "Not Found" | Rota não encontrada |
| `Exception` (genérica) | 500 | "Internal Server Error" | Erro inesperado |

## 10. Códigos de Status HTTP

| Código | Uso |
|--------|-----|
| `200 OK` | Respostas bem-sucedidas (chat, health, listas) |
| `202 Accepted` | Ingestão de documento aceita para processamento |
| `204 No Content` | Delete bem-sucedido |
| `400 Bad Request` | Erro de validação simples (arquivo vazio, PDF corrompido) |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Sessão expirada |
| `413 Payload Too Large` | Arquivo muito grande |
| `415 Unsupported Media Type` | Tipo de arquivo não suportado |
| `422 Unprocessable Entity` | Erro de validação de negócio ou DTO |
| `500 Internal Server Error` | Erro interno inesperado |
| `502 Bad Gateway` | Serviço de IA (Ollama) indisponível |

## 11. Regras de Negócio Importantes

### Sessão
- A primeira chamada deve ser `GET /api/session` para obter um `sessionId`
- O `sessionId` deve ser um UUID v4 e é validado via regex
- Sessões expiram após 24h de inatividade
- Sessão expirada → `409 Conflict` em qualquer operação

### Chat
- O `content` da mensagem é limitado a 5000 caracteres
- Se `conversationId` é `null`, uma NOVA conversa é criada automaticamente
- O título da conversa são os primeiros 50 caracteres da primeira mensagem
- Mensagens são salvas antes e depois do processamento RAG
- Se o Ollama falhar, a mensagem do usuário já foi salva (mas não há resposta)
- O fluxo RAG é transacional: se falhar, tudo é rollback (incluindo a mensagem do usuário)

### Busca Semântica
- A similaridade é calculada como **cosseno**: `cos(a,b) = a·b / (|a|×|b|)`
- O pgvector usa distância cosseno (`<->`), que é `1 - cos`
- Queries curtas são melhores (o embedding model tem contexto limitado a 2048 tokens)
- O nomic-embed-text gera vetores de 768 dimensões

### Ingestão
- Arquivos de ingestão são limitados a **50 MB** (upload de anexo: **10 MB**)
- Cada chunk tem tamanho de **800 caracteres** com **120 de overlap**
- O overlap garante que contexto não seja perdido entre chunks
- Títulos são extraídos de `Título: ...` no texto ou da URL
- URLs internas (localhost, 10.x, 192.168.x, etc.) são bloqueadas por segurança
- O webhook n8n é chamado APÓS cada ingestão (sucesso ou falha)
- Retry do webhook: 3 tentativas com 1s de intervalo

### Prompt da IA
- A IA responde **APENAS sobre o Universo Marvel**
- Se a pergunta não for sobre Marvel: "Infelizmente esse conteúdo que você busca não faz parte do universo Marvel."
- Respostas em português do Brasil
- Se não houver chunks relevantes, a IA usa apenas o conhecimento base do MCU

## 12. Cabeçalhos CORS

A API libera CORS para qualquer origem:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: *
Access-Control-Allow-Credentials: true
```

**IMPORTANTE:** `AllowCredentials: true` com `AllowOrigin: *` requer que o frontend NÃO use `withCredentials` em requisições, ou a configuração precisa ser ajustada.

## 13. Testes

O projeto possui testes unitários e de integração:

```
src/test/java/com/project/chat/
├── ChatApplicationTests.java
├── controller/
│   ├── ChatControllerTest.java
│   ├── DocumentControllerTest.java
│   ├── HealthControllerTest.java
│   └── UploadControllerTest.java
├── service/
│   ├── DocumentIngestionServiceTest.java
│   ├── EmbeddingServiceTest.java
│   ├── RagChatServiceTest.java
│   ├── RetrievalServiceTest.java
│   ├── SimulatedChatServiceTest.java
│   ├── UploadServiceTest.java
│   └── parser/
│       ├── ParserFactoryTest.java
│       ├── PdfParserTest.java
│       └── TxtParserTest.java
└── repository/
    ├── ConversationRepositoryTest.java
    ├── DocumentChunkRepositoryTest.java
    └── MessageRepositoryTest.java
```

**Comando para rodar:** `mvn test` (no diretório `chat-backend/`)

**Observações:**
- Testes de repository usam PostgreSQL (ou H2 em profile dev)
- Testes de controller usam MockMvc + Mockito
- Testes de service mockam dependências externas (Ollama, repositórios)

## 14. Docker

O `docker-compose.yml` na raiz do projeto orquestra 4 serviços:

| Serviço | Container | Porta | Descrição |
|---------|-----------|-------|-----------|
| `ollama` | `ollama-marvel` | 11434 | LLM + Embeddings (gemma4 + nomic-embed-text) |
| `n8n` | `n8n-marvel` | 5678 | Workflow automation (webhook) |
| `postgres` | `postgres-marvel` | 5432 | PostgreSQL + pgvector |
| `app` | `chat-api-marvel` | 8080 | Spring Boot API |

**Rede interna:** `marvel-net` (bridge)

**Healthcheck:** Ollama depende de `ollama list`, PostgreSQL depende de `pg_isready`

**Variáveis de ambiente da app:**
```yaml
SPRING_PROFILES_ACTIVE=rag
RAG_OLLAMA_URL=http://ollama:11434
OLLAMA_MODEL=gemma4
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/chatdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
RAG_WEBHOOK_URL=http://n8n:5678/webhook/ingestion-complete
```

---

## Configuração Local para Desenvolvimento

### Pré-requisitos
- Java 17+
- Maven 3.9+
- PostgreSQL 16+ com pgvector
- Ollama rodando localmente na porta 11434

### Passos
```bash
# 1. Iniciar PostgreSQL (via Docker):
docker run -d --name postgres-pgvector -p 5432:5432 \
  -e POSTGRES_DB=chatdb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  pgvector/pgvector:pg16

# 2. Baixar modelos Ollama:
ollama pull gemma4
ollama pull nomic-embed-text

# 3. Iniciar app:
cd chat-backend
mvn spring-boot:run -Dspring-boot.run.profiles=rag

# 4. API disponível em http://localhost:8080
```

### Verificação Rápida
```bash
# Health check
curl http://localhost:8080/api/health

# Criar sessão
curl -X GET http://localhost:8080/api/session

# Enviar mensagem (substituir sessionId)
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"SEU-UUID","content":"Quem é o Homem de Ferro?"}'
```

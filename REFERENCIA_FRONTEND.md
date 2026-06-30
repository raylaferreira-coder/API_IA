# API — Guia de Referência para o Frontend

> Backend: Spring Boot 3.x | Java 17 | PostgreSQL + pgvector | Ollama (gemma3:4b + nomic-embed-text)

---

## Sumário

1. [Base URL & CORS](#1-base-url--cors)
2. [Autenticação (Sessão)](#2-autenticação-sessão)
3. [Chat — Fluxo Síncrono](#3-chat--fluxo-síncrono)
4. [Chat — Fluxo Assíncrono (Pooling)](#4-chat--fluxo-assíncrono-pooling)
5. [Chat — Fluxo Streaming (SSE)](#5-chat--fluxo-streaming-sse)
6. [Chat — Upload-and-Ask](#6-chat--upload-and-ask)
7. [Chat — Histórico](#7-chat--histórico)
8. [Documentos — Ingestão](#8-documentos--ingestão)
9. [Documentos — Busca Semântica](#9-documentos--busca-semântica)
10. [Health Check](#10-health-check)
11. [DTOs Completos — Request/Response](#11-dtos-completos--requestresponse)
12. [Entidades do Banco](#12-entidades-do-banco)
13. [Códigos de Erro](#13-códigos-de-erro)
14. [Checklist de Integração React](#14-checklist-de-integração-react)

---

## 1. Base URL & CORS

| Propriedade | Valor |
|---|---|
| **Base URL** | `http://localhost:8080` |
| **CORS** | `*` (qualquer origem, qualquer header) |
| **Métodos liberados** | GET, POST, PUT, DELETE, OPTIONS |
| **Credenciais** | Permitidas |

---

## 2. Autenticação (Sessão)

Não há login/JWT. O frontend cria uma sessão UUID v4 e a envia em toda requisição.

### `POST /api/session`

Cria uma nova sessão.

**Response `200 OK`:**

```json
{
  "sessionId": "uuid-v4",
  "createdAt": "2026-06-30T12:00:00.000Z",
  "lastActivity": "2026-06-30T12:00:00.000Z",
  "expired": false
}
```

**Uso no React:** gerar sessão ao iniciar o app e **persistir em `localStorage`**. Enviar `sessionId` em toda requisição de chat.

### `GET /api/session/{sessionId}`

Retorna dados da sessão. Usado para verificar se a sessão expirou (24h de inatividade).

### `DELETE /api/session/{sessionId}`

Invalida a sessão (opcional — usado em "logout").

---

## 3. Chat — Fluxo Síncrono

Único request → resposta completa. **~23 segundos** (RAG + Ollama local). A thread do frontend deve **aguardar** a resposta.

### `POST /api/chat/message`

**Request:**

```json
{
  "sessionId": "uuid-v4",
  "conversationId": null,
  "content": "Quem é o Homem de Ferro?",
  "attachmentId": null
}
```

| Campo | Obrigatório | Descrição |
|---|---|---|
| `sessionId` | Sim | UUID v4 da sessão |
| `conversationId` | Não | `null` para criar nova conversa; enviar ID para continuar |
| `content` | Sim | Pergunta (max 5000 caracteres) |
| `attachmentId` | Não | ID de attachment pré-upload (ver upload-and-ask) |

**Response `200 OK`:**

```json
{
  "userMessage": {
    "id": 1,
    "conversationId": 10,
    "role": "USER",
    "content": "Quem é o Homem de Ferro?",
    "timestamp": "2026-06-30T12:00:00.000Z",
    "attachment": null
  },
  "assistantMessage": {
    "id": 2,
    "conversationId": 10,
    "role": "ASSISTANT",
    "content": "Tony Stark... [resposta gerada pelo Ollama]",
    "timestamp": "2026-06-30T12:00:01.234Z",
    "attachment": null
  },
  "conversationId": 10
}
```

**Fluxo interno:** Sessão → Criação/Busca da conversa → Salva mensagem USER → Embedding (Ollama) → Busca pgvector → Monta prompt → LLM (Ollama) → Salva resposta ASSISTANT → Retorna.

---

## 4. Chat — Fluxo Assíncrono (Pooling)

O frontend **não aguarda**. Recebe um `taskId` e deve **polling** até o status ser `COMPLETED`.

### `POST /api/chat/message/async`

**Request:** igual ao síncrono.

**Response `202 Accepted`:**

```json
{
  "taskId": "uuid-v4",
  "status": "PENDING",
  "createdAt": "2026-06-30T12:00:00.000Z"
}
```

### `GET /api/chat/message/async/{taskId}`

Polling.

**Response `200 OK` (PENDING/PROCESSING):**

```json
{
  "taskId": "uuid-v4",
  "status": "PROCESSING",
  "createdAt": "2026-06-30T12:00:00.000Z",
  "completedAt": null,
  "result": null,
  "errorMessage": null
}
```

**Response `200 OK` (COMPLETED):**

```json
{
  "taskId": "uuid-v4",
  "status": "COMPLETED",
  "createdAt": "2026-06-30T12:00:00.000Z",
  "completedAt": "2026-06-30T12:00:23.000Z",
  "result": {
    "userMessage": { ... },
    "assistantMessage": { ... },
    "conversationId": 10
  },
  "errorMessage": null
}
```

**Response `200 OK` (FAILED):**

```json
{
  "taskId": "uuid-v4",
  "status": "FAILED",
  "errorMessage": "O serviço de IA local está indisponível."
}
```

**Estados possíveis:** `PENDING` → `PROCESSING` → `COMPLETED` | `FAILED`.

**Intervalo de polling sugerido:** 2 segundos.

---

## 5. Chat — Fluxo Streaming (SSE)

Resposta entregue em **eventos Server-Sent Events (SSE)**. O frontend abre uma conexão e escuta eventos nomeados.

### `POST /api/chat/message/stream`

**Request:** igual ao síncrono.

**Content-Type:** `text/event-stream`

**Eventos SSE:**

| Evento | Quando | Dados |
|---|---|---|
| `connected` | Imediato | `{"type":"connected"}` |
| `token` | A cada token gerado | `{"type":"token","content":"Tony"}` |
| `done` | Fim da resposta | `{"type":"done","userMessage":{...},"assistantMessage":{...},"conversationId":10}` |
| `error` | Erro | `{"type":"error","content":"Erro ao gerar resposta: ..."}` |

**Exemplo de consumo no React (EventSource):**

```javascript
// NOTA: POST com body não funciona com EventSource nativo.
// Use fetch + ReadableStream:

const response = await fetch('http://localhost:8080/api/chat/message/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ sessionId, content: 'Quem é o Homem de Ferro?' })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  const chunk = decoder.decode(value);
  // Parse SSE lines: "event: token\ndata: {...}\n\n"
  // Extrair event e data, processar conforme o tipo
}
```

---

## 6. Chat — Upload-and-Ask

Envia um arquivo + pergunta opcional. O arquivo é parseado (PDF, imagem, TXT, etc) e o conteúdo é injetado como contexto para o LLM.

### `POST /api/chat/upload-and-ask`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `file` | File | Sim | Arquivo (PDF, TXT, MD, HTML, DOCX, JPG, PNG, BMP, TIFF, GIF) |
| `sessionId` | String | Sim | UUID da sessão |
| `conversationId` | Number | Não | Para continuar conversa existente |
| `content` | String | Não | Pergunta sobre o arquivo. Se vazio: imagem → "Descreva esta imagem"; documento → "Resuma este documento" |

**Tamanho máximo:** **10 MB** (50 MB na ingestão de documentos).

**Response:** igual ao `POST /api/chat/message` (chatResponse).

**Fluxo:** Salva arquivo → Detecta tipo → Se imagem: OCR + descrição visual (Ollama vision). Se documento: parse → chunk → embedding + indexação → Pergunta + conteúdo extraído → LLM → Resposta.

---

## 7. Chat — Histórico

### `GET /api/chat/history/{sessionId}`

Lista as conversas de uma sessão.

**Response `200 OK`:**

```json
{
  "sessionId": "uuid-v4",
  "conversations": [
    {
      "id": 10,
      "title": "Quem é o Homem de Ferro?...",
      "messageCount": 2,
      "lastMessage": "Tony Stark...",
      "lastActivity": "2026-06-30T12:00:01.234Z"
    }
  ]
}
```

### `GET /api/chat/history/{sessionId}/{conversationId}`

Retorna todas as mensagens de uma conversa.

**Response `200 OK`:**

```json
{
  "id": 10,
  "messages": [
    {
      "id": 1,
      "conversationId": 10,
      "role": "USER",
      "content": "Quem é o Homem de Ferro?",
      "timestamp": "2026-06-30T12:00:00.000Z",
      "attachment": null
    },
    {
      "id": 2,
      "conversationId": 10,
      "role": "ASSISTANT",
      "content": "Tony Stark...",
      "timestamp": "2026-06-30T12:00:01.234Z",
      "attachment": null
    }
  ]
}
```

---

## 8. Documentos — Ingestão

### `POST /api/documents/ingest`

Upload de arquivo para indexar na base vetorial (pgvector).

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `file` | File | Sim | PDF, TXT, MD, HTML, DOCX, JPG, PNG, BMP, TIFF, GIF |
| `sourceType` | String | Não | Forçar tipo (pdf, txt, markdown, html, docx, jpg, png, etc). Se omitido, detectado pela extensão |

**Tamanho máximo:** **50 MB**.

**Response `202 Accepted`:**

```json
{
  "documentId": 1,
  "fileName": "documento.pdf",
  "status": "PROCESSING",
  "chunks": 0,
  "processingTime": 0,
  "message": "Documento enviado para processamento."
}
```

O processamento é síncrono. Quando concluído, o `status` muda para `COMPLETED`. O webhook n8n (se configurado) é chamado ao final.

### `POST /api/documents/ingest/url`

Ingere conteúdo de uma URL.

**Request:**

```json
{
  "url": "https://exemplo.com/marvel"
}
```

### `GET /api/documents`

Lista todos os documentos indexados.

### `GET /api/documents/{id}`

Detalhes de um documento.

### `DELETE /api/documents/{documentId}`

Remove documento e seus chunks.

### `GET /api/documents/{documentId}/chunks`

Retorna os chunks de um documento.

### `POST /api/documents/search`

Busca semântica nos documentos.

**Request:**

```json
{
  "query": "Quem é Thanos?",
  "topK": 5
}
```

**Response `200 OK`:**

```json
{
  "results": [
    {
      "chunkId": 42,
      "documentId": 1,
      "fileName": "marvel.pdf",
      "chunkIndex": 3,
      "content": "Thanos é um titã...",
      "similarityScore": 0.89
    }
  ]
}
```

---

## 9. Documentos — Busca Semântica

A busca semântica no endpoint `POST /api/documents/search` usa o embedding da pergunta para encontrar os chunks mais similares no pgvector (similaridade cosseno). `topK` padrão = 5, configurável.

---

## 10. Health Check

### `GET /api/health`

```json
{
  "status": "UP",
  "database": "UP",
  "ollama": "UP",
  "diskSpace": "OK (50 GB disponível)",
  "timestamp": "2026-06-30T12:00:00.000Z",
  "version": "2.0.0"
}
```

| status | Significado |
|---|---|
| `UP` | Banco + Ollama OK |
| `DEGRADED` | Um dos serviços falhou |
| `DOWN` | Banco falhou |

### `GET /api/health/liveness`

Apenas verifica o banco (ignora Ollama). Usado para Kubernetes/Docker health probe.

---

## 11. DTOs Completos — Request/Response

### Requests

#### `ChatRequest`

```json
{
  "sessionId": "string (UUID, obrigatório)",
  "conversationId": "number (opcional)",
  "content": "string (1-5000 caracteres, obrigatório)",
  "attachmentId": "number (opcional)"
}
```

#### `IngestUrlRequest`

```json
{
  "url": "string (URL válida, obrigatório)"
}
```

#### `SearchRequest`

```json
{
  "query": "string (obrigatório)",
  "topK": "number (opcional, default 5)"
}
```

#### `UploadAndAskRequest`

(Não é JSON — é multipart/form-data, vide seção 6)

### Responses

#### `ChatResponse`

```json
{
  "userMessage": "MessageResponse",
  "assistantMessage": "MessageResponse",
  "conversationId": "number"
}
```

#### `MessageResponse`

```json
{
  "id": "number",
  "conversationId": "number",
  "role": "USER | ASSISTANT",
  "content": "string",
  "timestamp": "ISO datetime",
  "attachment": "AttachmentResponse | null"
}
```

#### `SessionResponse`

```json
{
  "sessionId": "string (UUID)",
  "createdAt": "ISO datetime",
  "lastActivity": "ISO datetime",
  "expired": "boolean"
}
```

#### `HistoryResponse`

```json
{
  "sessionId": "string",
  "conversations": ["ConversationSummaryResponse"]
}
```

#### `ConversationSummaryResponse`

```json
{
  "id": "number",
  "title": "string",
  "messageCount": "number",
  "lastMessage": "string",
  "lastActivity": "ISO datetime"
}
```

#### `ConversationResponse`

```json
{
  "id": "number",
  "messages": ["MessageResponse"]
}
```

#### `TaskStatusResponse`

```json
{
  "taskId": "string (UUID)",
  "status": "PENDING | PROCESSING | COMPLETED | FAILED",
  "result": "ChatResponse | null",
  "errorMessage": "string | null",
  "createdAt": "string (ISO)",
  "completedAt": "string (ISO) | null"
}
```

#### `IngestionResponse`

```json
{
  "documentId": "number",
  "fileName": "string",
  "status": "PENDING | PROCESSING | COMPLETED | FAILED",
  "chunks": "number",
  "processingTime": "number (ms)",
  "message": "string"
}
```

#### `DocumentResponse`

```json
{
  "id": "number",
  "fileName": "string",
  "sourceType": "string",
  "fileSize": "number",
  "status": "PENDING | PROCESSING | COMPLETED | FAILED",
  "totalChunks": "number",
  "createdAt": "ISO datetime"
}
```

#### `DocumentsListResponse`

```json
{
  "documents": ["DocumentResponse"]
}
```

#### `DocumentChunkResponse`

```json
{
  "chunkId": "number",
  "documentId": "number",
  "fileName": "string",
  "chunkIndex": "number",
  "content": "string",
  "similarityScore": "number (0-1)"
}
```

#### `SearchResultResponse`

```json
{
  "results": ["DocumentChunkResponse"]
}
```

#### `AttachmentResponse`

```json
{
  "id": "number",
  "fileName": "string",
  "fileType": "string",
  "fileSize": "number",
  "uploadedAt": "ISO datetime"
}
```

#### `HealthResponse`

```json
{
  "status": "UP | DEGRADED | DOWN",
  "database": "UP | DOWN",
  "ollama": "UP | DOWN",
  "diskSpace": "string",
  "timestamp": "ISO datetime",
  "version": "string"
}
```

#### `ErrorResponse`

```json
{
  "status": "number (HTTP status)",
  "error": "string",
  "message": "string",
  "timestamp": "ISO datetime",
  "path": "string",
  "errors": [
    {
      "field": "string",
      "message": "string"
    }
  ]
}
```

---

## 12. Entidades do Banco

```
sessions
├── id (PK, BIGINT)
├── session_id (UUID, UNIQUE)
├── created_at (TIMESTAMP)
├── last_activity (TIMESTAMP)
└── expired (BOOLEAN)

conversations
├── id (PK, BIGINT)
├── session_id (FK → sessions.id)
├── title (VARCHAR)
├── created_at (TIMESTAMP)
├── updated_at (TIMESTAMP)
├── active (BOOLEAN)
└── messages (OneToMany)

messages
├── id (PK, BIGINT)
├── conversation_id (FK → conversations.id)
├── role (ENUM: USER, ASSISTANT)
├── content (TEXT)
├── timestamp (TIMESTAMP)
└── attachment (OneToOne → attachments)

attachments
├── id (PK, BIGINT)
├── message_id (FK → messages.id, nullable)
├── file_name (VARCHAR)
├── file_type (VARCHAR)
├── file_size (BIGINT)
├── storage_path (VARCHAR)
└── uploaded_at (TIMESTAMP)

documents
├── id (PK, BIGINT)
├── title/file_name (VARCHAR)
├── source_url (TEXT)
├── source_type (VARCHAR)
├── file_size (BIGINT)
├── status (ENUM: PENDING, PROCESSING, COMPLETED, FAILED)
├── error_message (TEXT)
├── chunk_count (INT)
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)

document_chunks
├── id (PK, BIGINT)
├── document_id (FK → documents.id)
├── chunk_index (INT)
├── content (TEXT)
├── embedding (VECTOR(768))  ← pgvector
├── token_count (INT)
└── created_at (TIMESTAMP)
```

---

## 13. Códigos de Erro

| HTTP | `error` | Causa comum |
|---|---|---|
| 400 | Bad Request | Arquivo vazio, argumento inválido, arquivo corrompido |
| 404 | Not Found | Sessão, conversa ou documento não encontrado |
| 409 | Conflict | Sessão expirada |
| 413 | Payload Too Large | Arquivo excede limite (10 MB upload-and-ask, 50 MB ingest) |
| 415 | Unsupported Media Type | Tipo de arquivo não suportado |
| 422 | Unprocessable Entity | Validação de campos, falha de ingestão |
| 502 | Bad Gateway | Ollama indisponível ou erro de embedding |

**Formato do erro:**

```json
{
  "status": 502,
  "error": "Bad Gateway",
  "message": "O serviço de inteligência artificial está temporariamente indisponível. Verifique se o Ollama está em execução.",
  "timestamp": "2026-06-30T12:00:00.000Z",
  "path": "/api/chat/message"
}
```

---

## 14. Checklist de Integração React

### Sessão
- [ ] Chamar `POST /api/session` ao montar o app
- [ ] Persistir `sessionId` no `localStorage`
- [ ] Recarregar `sessionId` do `localStorage` em toda inicialização
- [ ] Chamar `GET /api/session/{id}` para verificar expiração
- [ ] Se expirada, criar nova sessão

### Chat
- [ ] `POST /api/chat/message` → resposta síncrona (~23s)
- [ ] `POST /api/chat/message/async` → polling a cada 2s
- [ ] `POST /api/chat/message/stream` → SSE com fetch + ReadableStream
- [ ] Atualizar UI a cada evento `token`
- [ ] Finalizar UI no evento `done`

### Upload de Arquivo
- [ ] `POST /api/chat/upload-and-ask` → multipart/form-data
- [ ] Validar tamanho: **10 MB** para chat, **50 MB** para ingestão
- [ ] Tipos aceitos: pdf, txt, md, html, docx, jpg, png, bmp, tiff, gif

### Histórico
- [ ] `GET /api/chat/history/{sessionId}` → listar conversas
- [ ] `GET /api/chat/history/{sessionId}/{conversationId}` → carregar mensagens

### Documentos (Admin/Config)
- [ ] `POST /api/documents/ingest` → upload para indexação
- [ ] `POST /api/documents/ingest/url` → URL para indexação
- [ ] `GET /api/documents` → listar documentos
- [ ] `DELETE /api/documents/{id}` → remover
- [ ] `POST /api/documents/search` → busca semântica

### Health Check
- [ ] `GET /api/health` → status geral do backend
- [ ] Exibir indicador visual (UP/DEGRADED/DOWN) na UI

### Boas Práticas
- [ ] Mostrar loading durante ~23s no modo síncrono
- [ ] Exibir tokens incrementalmente no modo streaming
- [ ] Tratar erro 502 (Ollama offline) com mensagem amigável
- [ ] Não enviar `content` vazio
- [ ] Usar `conversationId` retornado para continuar a conversa
- [ ] Formatar timestamp ISO no fuso local do usuário

---

> **Observação:** Caso o webhook n8n esteja habilitado e configurado, após cada ingestão de documento o backend notifica `POST http://localhost:5678/webhook/ingestion-complete` com payload `{ documentId, fileName, status, chunks, embeddingModel, processingTime, timestamp }`.

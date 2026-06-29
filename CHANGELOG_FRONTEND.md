# Changelog — Alterações no Backend para o Frontend

## 1. Novo Endpoint: Upload-and-Ask

Envia um arquivo e já recebe a resposta do assistente em uma única chamada.

### `POST /api/chat/upload-and-ask`
**Content-Type:** `multipart/form-data`

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `file` | arquivo | sim | Arquivo (documento ou imagem) |
| `sessionId` | string | sim | ID da sessão |
| `conversationId` | number | não | ID da conversa existente (null = nova conversa) |
| `content` | string | não | Mensagem de texto opcional |

**Resposta (200):**
```json
{
  "message": "Resposta do assistente com base no arquivo enviado.",
  "sessionId": "abc-123",
  "conversationId": 5,
  "timestamp": "2026-06-28T21:00:00.000Z"
}
```

### Funcionamento por tipo de arquivo

| Tipo | Processamento |
|------|---------------|
| `.txt`, `.pdf`, `.md`, `.html`, `.docx` | Extrai texto → chunking → embedding → salva no RAG → pergunta ao LLM |
| `.jpg`, `.jpeg`, `.png`, `.bmp`, `.tiff`, `.gif` | OCR extrai texto + modelo llava descreve a imagem → contexto combinado → pergunta ao LLM |

---

## 2. Endpoints Existentes (inalterados)

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/chat/message` | Enviar mensagem de texto |
| `GET` | `/api/chat/history/{sessionId}` | Listar conversas |
| `GET` | `/api/chat/history/{sessionId}/{conversationId}` | Ver mensagens de uma conversa |
| `POST` | `/api/documents/ingest` | Upload de documento (agora aceita .docx e imagens) |
| `POST` | `/api/documents/ingest/url` | Ingestão via URL |
| `GET` | `/api/documents` | Listar documentos |
| `DELETE` | `/api/documents/{id}` | Remover documento |

---

## 3. Novos tipos de arquivo aceitos na ingestão (`POST /api/documents/ingest`)

- **Documentos:** `.docx` (Word)
- **Imagens:** `.jpg`, `.jpeg`, `.png`, `.bmp`, `.tiff`, `.tif`, `.gif`

### Limites
- Upload normal (`/api/chat/upload-and-ask`): **10 MB**
- Ingestão de documentos (`/api/documents/ingest`): **50 MB**

---

## 4. O que o Frontend pode fazer com isso

### Fluxo sugerido para upload-and-ask
1. Usuário seleciona um arquivo (documento ou imagem)
2. Frontend faz `POST /api/chat/upload-and-ask` com `multipart/form-data`
3. Backend processa o arquivo e retorna a resposta do assistente
4. Exibir a resposta na interface de chat normalmente

### Observações
- O parâmetro `content` é opcional — pode enviar só o arquivo sem texto
- Se o usuário digitar uma mensagem junto com o arquivo, envie no campo `content`
- O documento enviado via `upload-and-ask` também é indexado no RAG automaticamente, então futuras perguntas podem encontrar informações dele

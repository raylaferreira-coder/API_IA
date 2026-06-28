# AGENTS.md — Contexto para Geração do Back-end (v2.0.0)

## Objetivo

Gerar a API REST em Java 17+ com Spring Boot 3.x para o Assistente Inteligente especializado no Universo Cinematográfico Marvel (MCU) com sistema RAG completo.

## Escopo

- Chat com respostas geradas por RAG (Retrieval-Augmented Generation)
- Pipeline de ingestão: Upload → Parser → Chunking → Embedding → pgvector → Webhook n8n
- Suporte a PDF, TXT, Markdown, HTML e URLs
- Modelos locais via Ollama (proibido usar OpenAI ou APIs pagas)
- PostgreSQL com pgvector para busca semântica
- Clean Architecture com Strategy Pattern para parsing

## Tecnologias

- Java 17+, Spring Boot 3.x, Spring Data JPA, Hibernate 6
- PostgreSQL 15+ com pgvector
- Ollama (llama3.2 + nomic-embed-text)
- Apache PDFBox
- n8n
- Docker + Docker Compose
- JUnit 5, Mockito, MockMvc
- Maven 3.9+

## Regras de Implementação

1. **Controllers** não contêm regra de negócio — apenas delegam para Services.
2. **Services** implementam interfaces — não dependem de HTTP.
3. **Repositories** estendem `JpaRepository` — sem lógica condicional.
4. **Entities** usam `Long` como ID, `LocalDateTime` para timestamps.
5. **DTOs** separados em `request/` e `response/` — não expor entities na API.
6. **Exception** hierarchy + `GlobalExceptionHandler` com `@RestControllerAdvice`.
7. **Mappers** convertem Entity ↔ DTO.
8. **Parsers** seguem Strategy Pattern — nunca dentro de Controller ou EmbeddingService.
9. **EmbeddingService** é completamente agnóstico — recebe textos, devolve vetores.
10. **DocumentIngestionService** NÃO conversa com o LLM — usa apenas embedding.
11. Chunking configurável via `application.yml` (chunk-size: 800, overlap: 120) — nunca hardcoded.
12. Embeddings armazenados em coluna VECTOR do pgvector — nunca em JSON.
13. Webhook n8n após ingestão: POST para URL configurável, payload com documentId/fileName/status/chunks.

## Estrutura de Pacotes

```
com.project.chat
├── controller
├── service
├── parser          # Strategy Pattern
├── repository
├── entity
├── dto (request/, response/)
├── config
├── exception
├── mapper
└── util
```

## Contrato da API

Ver seção 4 (API REST) e seção 5 (Contratos JSON) do documento SYSTEM_DOCS_BACKEND.md.

## Pontos de Extensão

- `ChatService` é uma interface. `SimulatedChatService` (simulado) e `RagChatService` (RAG real) são implementações.
- Para alternar entre simulado e RAG: utilizar perfis Spring (`dev` vs `rag`).
- Novos parsers: implementar `DocumentParser` e registrar na `ParserFactory`.
- Novos modelos de embedding: alterar configuração no `application.yml`.

# Relatório Final de QA — Assistente MCU RAG

## Resumo

| Etapa | Descrição | Status |
|-------|-----------|--------|
| 1 | Backend inicia (Spring Boot + pgvector + Ollama) | ✅ |
| 2 | Frontend acessível (porta 3000/Caddy) | ✅ |
| 3 | Criação de sessão (`POST /api/session`) | ✅ |
| 4 | Mensagem simples (`POST /api/chat/message`) | ✅ |
| 5 | Upload TXT (`POST /api/documents/ingest`) | ✅ |
| 6 | RAG query com contexto do documento | ✅ |
| 7 | Upload PDF | ✅ |
| 8 | Upload imagem (OCR graceful failure) | ✅ |
| 9 | Todos os endpoints validados | ✅ |
| 10 | Relatório final gerado | ✅ |

## Endpoints Validados

| Método | Rota | Status | Detalhes |
|--------|------|--------|----------|
| GET | `/api/health` | 200 | UP, DB UP, Ollama UP |
| GET | `/api/health/liveness` | 200 | Liveness check |
| POST | `/api/session` | 201 | Sessão UUID criada |
| GET | `/api/session/{id}` | 200 | Dados da sessão |
| POST | `/api/chat/message` | 200 | Resposta RAG gerada |
| GET | `/api/documents` | 200 | Lista documentos |
| POST | `/api/documents/ingest` | 200 | Upload processado |

## Correções Aplicadas (Bugs)

### Bug #1 — SessionController (`SessionController.java:19`)
- **Problema**: `createSession()` anotado com `@GetMapping` em vez de `@PostMapping`.
- **Impacto**: Chamada POST retornava 405 Method Not Allowed.
- **Correção**: Substituído `@GetMapping` por `@PostMapping`.

### Bug #2 — FileUtils.MAX_FILE_SIZE (`FileUtils.java:23`)
- **Problema**: Constante `MAX_FILE_SIZE = 50 * 1024 * 1024` (50 MB), mas mensagem de erro dizia "10 MB".
- **Impacto**: Inconsistência entre validação e feedback ao usuário.
- **Correção**: Alterado para `10L * 1024 * 1024` (10 MB).

### Bug #3 — ChunkService loop infinito (`ChunkService.java:50-52`)
- **Problema**: Quando `end >= length` no último chunk, `start = end - overlap` recuava o ponteiro, criando loop infinito que consumia toda a heap (OutOfMemoryError).
- **Impacto**: Qualquer upload de documento causava OOM no container Docker.
- **Correção**: Adicionado `if (end >= length) break;` antes do recálculo do `start`.

### Bug #4 — ImageOcrParser não captura UnsatisfiedLinkError (`ImageOcrParser.java:40`)
- **Problema**: `UnsatisfiedLinkError` (Tesseract não instalado) não é subclasse de `Exception`, então não era capturado.
- **Impacto**: Upload de imagem retornava 500 em vez de erro gracioso.
- **Correção**: Substituído `catch (Exception e)` por `catch (Throwable e)`.

### Melhoria — Dockerfile JVM Heap (`Dockerfile:26`)
- **Problema**: Container sem `-Xmx`, JVM usava heap padrão insuficiente.
- **Correção**: Adicionado `-Xmx1g` ao entrypoint.

## Evidências de Testes

- **Upload TXT**: `documentId=4, chunks=1, status=COMPLETED`
- **Upload PDF**: `documentId=5, chunks=1, status=COMPLETED`
- **Upload PNG**: `documentId=7, chunks=0, status=COMPLETED`
- **Resposta RAG**: 684 caracteres, conteúdo correto sobre Tony Stark/Homem de Ferro
- **Tempo de resposta**: ~23s (vs ~57s antes das correções)
- **Documentos no banco**: 7 registros (3 PROCESSING, 3 COMPLETED, 1 PROCESSING)

# Marvel IA — Backend RAG

Sistema de chat inteligente com suporte a documentos, utilizando **RAG (Retrieval-Augmented Generation)** para responder perguntas sobre o Universo Cinematográfico Marvel (MCU).

## Arquitetura

```
                    ┌─────────────┐
                    │  Frontend   │
                    │  (React)    │
                    └──────┬──────┘
                           │ HTTP
                    ┌──────▼──────┐
                    │   Backend   │
                    │  Spring Boot│
                    │   Java 17   │
                    └──┬───┬───┬──┘
                       │   │   │
              ┌────────┘   │   └────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │PostgreSQL │ │  Ollama  │ │   n8n    │
        │+ pgvector │ │ LLM + Emb│ │Automação │
        └──────────┘ └──────────┘ └──────────┘
```

## Stack

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.4.5 |
| Banco | PostgreSQL 16 + pgvector |
| LLM | Ollama (Gemma 3, nomic-embed-text) |
| Automação | n8n |
| Frontend | React (container separado) |
| Build | Maven |
| Container | Docker |

## Estrutura

```
BACKEND/
├── chat-backend/           # Aplicação Spring Boot
│   ├── src/
│   │   ├── main/java/com/project/chat/
│   │   │   ├── config/         # Configurações (CORS, Ollama, Storage)
│   │   │   ├── controller/     # Endpoints REST
│   │   │   ├── dto/            # Request/Response DTOs
│   │   │   ├── entity/         # Entidades JPA
│   │   │   ├── exception/      # Exceções e handlers
│   │   │   ├── mapper/         # Mappers (MapStruct)
│   │   │   ├── repository/     # Repositórios JPA
│   │   │   ├── service/        # Lógica de negócio
│   │   │   │   ├── marvel/     # Marvel API ingestion
│   │   │   │   └── parser/     # Parsers de documentos
│   │   │   └── util/           # Utilitários
│   │   ├── main/resources/     # Configs YAML, migrations
│   │   └── test/               # Testes unitários
│   ├── ai-agent/               # Módulo do Agente IA
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml          # Orquestração de serviços
├── knowledge/                  # Base de conhecimento inicial
├── uploads/                    # Uploads de usuário (runtime)
├── n8n/                        # Workflows n8n
├── .env.example                # Variáveis de ambiente
└── README.md
```

## Como Executar

### Pré-requisitos

- Docker e Docker Compose
- Java 17 (para desenvolvimento local)
- Maven 3.9+ (para desenvolvimento local)

### Com Docker

```bash
# Subir todos os serviços
docker compose up -d

# Verificar logs
docker compose logs -f

# Parar
docker compose down
```

### Local (sem Docker)

```bash
# Requer PostgreSQL e Ollama rodando localmente

cd chat-backend
mvn clean package -DskipTests
java -jar target/chat-backend-2.0.0.jar --spring.profiles.active=rag
```

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `rag` | Perfil ativo |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/chatdb` | URL do banco |
| `DATABASE_USERNAME` | `postgres` | Usuário do banco |
| `DATABASE_PASSWORD` | `postgres` | Senha do banco |
| `RAG_OLLAMA_URL` | `http://localhost:11434` | URL do Ollama |
| `OLLAMA_MODEL` | `gemma3:4b` | Modelo LLM |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | Modelo de embedding |
| `AI_AGENT_ENABLED` | `true` | Habilitar agente IA |
| `CHUNK_SIZE` | `1000` | Tamanho do chunk |
| `CHUNK_OVERLAP` | `150` | Overlap entre chunks |
| `N8N_WEBHOOK_URL` | `http://localhost:5678/webhook/ingestion-complete` | Webhook n8n |
| `MARVEL_API_PUBLIC_KEY` | — | Marvel API public key |
| `MARVEL_API_PRIVATE_KEY` | — | Marvel API private key |

## Build

```bash
cd chat-backend
mvn clean package
# Gera: target/chat-backend-2.0.0.jar
```

## Testes

```bash
cd chat-backend
mvn clean test
```

## Endpoints

### Health

| Método | Path | Descrição |
|---|---|---|
| GET | `/api/health` | Status geral (banco, Ollama, disco) |
| GET | `/api/health/liveness` | Liveness probe (K8s/Docker) |
| GET | `/api/health/readiness` | Readiness probe (K8s/Docker) |

### Sessão

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/session` | Criar nova sessão |
| GET | `/api/session/{sessionId}` | Obter sessão |
| DELETE | `/api/session/{sessionId}` | Invalidar sessão |

### Chat

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/chat/message` | Enviar mensagem (RAG) |
| POST | `/api/chat/message/stream` | Chat com streaming SSE |
| POST | `/api/chat/message/async` | Enviar mensagem assíncrona |
| GET | `/api/chat/message/async/{taskId}` | Status da tarefa assíncrona |
| GET | `/api/chat/history/{sessionId}` | Histórico da sessão |
| GET | `/api/chat/history/{sessionId}/{conversationId}` | Conversa específica |
| POST | `/api/chat/upload-and-ask` | Upload + pergunta |

### Documentos

| Método | Path | Descrição |
|---|---|---|
| POST | `/api/upload` | Upload de arquivo simples |
| POST | `/api/documents/ingest` | Upload e indexar documento |
| POST | `/api/documents/ingest/url` | Indexar documento por URL |
| GET | `/api/documents` | Listar documentos |
| GET | `/api/documents/{id}` | Obter documento |
| DELETE | `/api/documents/{documentId}` | Excluir documento |
| GET | `/api/documents/{documentId}/chunks` | Obter chunks do documento |
| POST | `/api/documents/search` | Busca vetorial semântica |

### Marvel Ingestion

| Método | Path | Descrição |
|---|---|---|
| GET | `/api/marvel/status` | Status das fontes Marvel |
| POST | `/api/marvel/ingest/fandom` | Ingerir personagens da Marvel Fandom |
| POST | `/api/marvel/ingest/fandom/page` | Ingerir página específica da Fandom |
| POST | `/api/marvel/ingest/wikipedia` | Ingerir personagens da Wikipedia |
| POST | `/api/marvel/ingest/wikipedia/page` | Ingerir página específica da Wikipedia |
| POST | `/api/marvel/ingest/marvel-api` | Ingerir da Marvel Developer API |
| POST | `/api/marvel/ingest/all` | Ingerir de todas as fontes |

## Módulos

### AI Agent (`ai-agent/`)

Agente especialista no Universo Marvel, implementa interface `AiAgent` com suporte a:
- Perguntas simples e com contexto
- Integração com Ollama
- Prompt builder especializado em MCU

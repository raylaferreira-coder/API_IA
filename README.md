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
faça download do repositório Back-end : https://github.com/raylaferreira-coder/API_IA.git
e Front-end: https://github.com/LeonamNgr/Trabalho-Inteligencia-Artificial-GenIA---Front-end-Grupo-5.git

Crie uma pasta com nome Marvel-IA, dentro cria duas pastas " BACKEND " E " FRONTEND " DENTRO DE FRONTEND CRIA UMA SEGUNDA PASTA COM MESMO NOME.

Dentro da segunda pasta " FRONTEND ", colo os arquicvos que baixou do repositorio do git, tudo que estiver dentro de Trabalho-Inteligencia-Artificial-GenIA---Front-end-Grupo-5, abra pasta chat-frontend, dentro dessa pasta abra um novo terminal e execute o comando   npm install

Apois baixar o repositorio do back-end, copie tudo que esta dentro da pasta API_IA, e cole na pasta que voce criou "BACKEND" .

FEITO ISSO : 

1.  Abra o PowerShell.

2.  Vá até a pasta onde está o docker-compose.yml:

Marvel-IA\BACKEND


3. Inicie os serviços:

docker compose up -d


4.  Verifique os containers:

docker ps

Containers esperados: - postgres-marvel - ollama-marvel - n8n-marvel -
chat-backend - chat-frontend

5.  Veja os logs do backend:

docker logs -f chat-backend

Quando aparecer: Started ChatApplication Tomcat started on port(s): 8080

pressione Ctrl+C.

6.  Teste o backend:

curl http://localhost:8080/api/health

curl http://localhost:8080/api/health/liveness

7.  Verifique os modelos do Ollama:

docker exec -it ollama-marvel ollama list

Modelos esperados: - gemma3:4b - nomic-embed-text

8. Abra o frontend:

http://localhost:3000

9. Faça um teste funcional:

-   Criar sessão
-   Enviar pergunta
-   Verificar resposta
-   Testar upload de PDF
-   Testar upload de imagem
-   Consultar histórico

10. Se alterar o código novamente:

docker compose down

docker compose build –no-cache

docker compose up -d

11. Se quiser apenas reiniciar:

docker compose restart

12. Para remover containers e volumes (apaga os dados):

docker compose down -v
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

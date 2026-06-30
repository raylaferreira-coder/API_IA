# Project Roadmap — Chat RAG Marvel v3

---

## Visão Geral

### Objetivo do Projeto

Transformar o Chat RAG Marvel de um monolito acoplado em uma **plataforma de IA modular, testável e preparada para produção**, seguindo Clean Architecture com Ports & Adapters, DDD e event-driven.

### Estado Atual

- Monolito Spring Boot 3.4.5 com pacotes por camada técnica
- RagChatService: God Object com 15+ dependências e 6+ responsabilidades
- Pipeline de ingestão replicado em 5 lugares
- MarvelIngestionController com regra de negócio
- Executors sem shutdown (vazamento de threads)
- Webhook síncrono bloqueando resposta
- TaskService em ConcurrentHashMap (perde dados no restart)
- 17 testes (cobertura baixa)
- AI Agent Module isolado e duplicando funcionalidade

### Estado Desejado (v3.0.0)

- 11 módulos Maven com boundaries claros
- Ports & Adapters (Hexagonal Architecture)
- Domínios ricos com comportamento encapsulado
- Testes unitários > 80% por módulo
- Testes de integração para todos os adapters
- Event-driven para webhooks e processamento assíncrono
- Resiliência com circuit breaker e rate limiting
- Observabilidade com métricas e tracing
- Plugin Marvel destacável (opcional no classpath)

### Cronograma

| Fase | Sprints | Duração | Entrega |
|---|---|---|---|
| **Fundação** | Sprint 0–1C | *A definir* | Infraestrutura + estrutura multi-módulo + Docker |
| **Chat Core** | Sprint 2-4 | 3 semanas | Chat domain + use cases + adapters JPA |
| **RAG + Document** | Sprint 5-6 | 2 semanas | RagEngine + DocumentEngine |
| **LLM + Vision** | Sprint 7-8 | 2 semanas | LLM Engine + Vision Engine |
| **Knowledge + Marvel** | Sprint 9-10 | 2 semanas | Knowledge Engine + Marvel Plugin |
| **Produção** | Sprint 11 | 1 semana | Resiliência + observabilidade |
| **TOTAL** | **14 sprints** | *A definir* | **v3.0.0** |

---

## Roadmap

---

### Sprint 0 — Fundação: Estrutura Multi-Módulo

**Duração:** 1 semana (dias 1-5)

**Objetivo:** Criar a estrutura Maven multi-módulo sem quebrar absolutamente nada no código existente. Tudo deve continuar compilando e rodando.

**Escopo:**

1. Criar `pom.xml` pai no diretório `chat-backend/` com `packaging=pom`
2. Criar módulo `shared-kernel` com exceções base e utilitários
3. Criar módulo `chat-core` vazio (apenas `pom.xml`)
4. Configurar dependências entre módulos no pom pai
5. Mover classes selecionadas do `util/` e `exception/` para `shared-kernel`
6. Configurar `spring-boot-maven-plugin` no módulo `core` (não no pai)
7. Verificar que `mvn clean compile` funciona
8. Verificar que `mvn clean package` gera JAR executável
9. Verificar que `docker-compose up` sobe o sistema
10. Executar testes existentes e confirmar que passam

**Arquivos afetados:**

```
chat-backend/pom.xml                          ← Criar (pom pai)
chat-backend/shared-kernel/pom.xml            ← Criar
chat-backend/shared-kernel/src/main/java/...  ← Mover exceptions + utils
chat-backend/chat-core/pom.xml               ← Criar
chat-backend/core/pom.xml                    ← Criar (bootstrap)
chat-backend/core/src/main/java/...          ← Mover ChatApplication.java
```

**Dependências:** Nenhuma (primeira sprint)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Maven multi-módulo quebra build existente | Alta | Crítico | Rodar `mvn clean verify` a cada commit, CI pipeline |
| `build-helper-maven-plugin` conflita com módulos | Média | Alto | Testar configuração em branch separada primeiro |
| Package scan do Spring não encontra beans | Alta | Alto | Configurar `@SpringBootApplication(scanBasePackages)` explicitamente |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `mvn clean compile` passa em 0 falhas
- [ ] `mvn clean test` passa todos os 17 testes existentes
- [ ] `mvn clean package -DskipTests` gera `core/target/*.jar`
- [ ] `docker-compose up --build` sobe e endpoints respondem
- [ ] `GET /api/health` retorna 200
- [ ] `POST /api/chat/message` funciona (fluxo feliz)

**Checklist do tech lead:**
- [ ] Decidir nome do groupId (manter `com.project.chat`)
- [ ] Decidir versionamento (semver: `3.0.0-SNAPSHOT`)
- [ ] Configurar `<parent>` no pom.xml de cada módulo
- [ ] Mover `ChatApplication.java` para `core` module
- [ ] Configurar `spring-boot-maven-plugin` apenas no `core`
- [ ] Adicionar `shared-kernel` como dependência de todos os módulos
- [ ] Remover `build-helper-maven-plugin` (incorporar ai-agent como módulo real)
- [ ] Testar build em Windows e Linux (CI)

**Resultado esperado:** Repositório com estrutura multi-módulo funcionando, todos os testes verdes, aplicação rodando em Docker sem alteração de comportamento.

---

### Sprint 1A — Infraestrutura Maven (sem modularização)

**Duração:** A definir

**Objetivo:** Padronizar a infraestrutura Maven do projeto sem alterar código Java, Docker ou estrutura de módulos. Preparar o terreno para a modularização futura.

**Escopo:**

1. Adicionar Maven Wrapper (`mvnw` + `mvnw.cmd` + `.mvn/`)
2. Padronizar encoding UTF-8:
   - `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`
   - `<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>`
3. Adicionar `maven-enforcer-plugin`:
   - Requer Java 17+
   - Requer Maven 3.9+
4. Organizar dependências do `pom.xml`:
   - Extrair versões hardcoded para propriedades (`<properties>`)
   - Criar seção `<dependencyManagement>` (se houver mais de um módulo futuro, preparar)
5. Padronizar versões dos plugins:
   - Centralizar versões de plugins em `<pluginManagement>`
   - Definir versão explícita do `spring-boot-maven-plugin`

**Restrições:**
- ❌ Não alterar Docker (Dockerfile, docker-compose)
- ❌ Não alterar código Java (.java)
- ❌ Não criar módulos Maven

**Arquivos afetados:**

```
chat-backend/pom.xml                          ← Modificar (properties, enforcer, wrapper, encoding)
chat-backend/mvnw                              ← Criar (Maven Wrapper)
chat-backend/mvnw.cmd                          ← Criar (Maven Wrapper)
chat-backend/.mvn/wrapper/maven-wrapper.properties  ← Criar
chat-backend/.mvn/jvm.config                   ← Opcional
```

**Dependências:** Sprint 0 (análise)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Maven Enforcer bloqueia build em versões antigas | Baixa | Alto | Testar em CI com versões mínimas |
| Maven Wrapper baixa versão diferente da CI | Baixa | Médio | Fixar versão no `.mvn/wrapper/maven-wrapper.properties` |
| Encoding UTF-8 expõe caracteres mal codificados em Windows | Baixa | Médio | Verificar arquivos .java com acentos antes |

**Estimativa:** A definir

**Critério de aceite:**
- [ ] `mvn clean verify` funciona (0 falhas, 0 warnings novos)
- [ ] `docker-compose up` funciona sem alterações nos arquivos Docker
- [ ] Maven Wrapper presente: `./mvnw --version` funciona
- [ ] `mvn enforcer:enforce` passa (Java 17+, Maven 3.9+)
- [ ] Nenhuma mudança de comportamento nos endpoints
- [ ] Nenhum arquivo .java alterado
- [ ] Nenhum arquivo Docker alterado
- [ ] Nenhum módulo criado

**Checklist:**
- [ ] Executar `mvn wrapper:wrapper` ou copiar `mvnw` manualmente
- [ ] Validar que `mvnw.cmd` funciona no PowerShell
- [ ] Verificar `file.encoding` no build log (deve ser UTF-8)
- [ ] Executar `mvn dependency:tree` para confirmar que dependências não mudaram

**Resultado esperado:** Build padronizado com wrapper, encoding explícito e validação de ambiente, sem qualquer alteração de código ou estrutura.

---

### Sprint 1B — Parent POM + Módulos Iniciais

**Duração:** A definir

**Objetivo:** Criar a estrutura Maven multi-módulo com POM pai e os módulos `core`, `api` e `ai-agent` (este último como módulo Maven real). Nenhum código é movido ainda — o código existente em `src/` permanece no lugar.

**Escopo:**

1. Criar `pom.xml` pai com `<packaging>pom</packaging>`:
   - Mover `<dependencyManagement>` para o pai
   - Mover `<pluginManagement>` para o pai
   - Declarar módulos: `core`, `api`, `ai-agent`
2. Criar módulo `core` (bootstrap Spring Boot):
   - `core/pom.xml` com `spring-boot-maven-plugin`
   - `core/src/main/java/com/project/chat/ChatApplication.java` (inicialmente vazio — o código real permanece em `src/`)
   - Fazer o módulo `core` referenciar `src/main/java` existente via `build-helper-maven-plugin` temporário OU usar `<compilerArgs>` para incluir diretório externo
3. Criar módulo `api` (controllers):
   - `api/pom.xml` — vazio, apenas estrutura
   - (código real permanece em `src/`)
4. Transformar `ai-agent` em módulo Maven real:
   - Criar `ai-agent/pom.xml` com as dependências necessárias
   - Manter código existente em `ai-agent/src/main/java/`
   - Remover referência ao `build-helper-maven-plugin` do pom.xml principal
5. Configurar dependências entre módulos:
   - `core` depende de `api`
   - `api` depende de `ai-agent` (se houver dependência real)

**Restrições:**
- ❌ Não mover código Java existente de `src/` para os módulos
- ❌ Não criar módulos além de `core`, `api`, `ai-agent`
- ❌ Não criar `rag-engine`, `document-engine`, `vision-engine` ou plugins
- ✅ `build-helper-maven-plugin` pode ser usado temporariamente no `core` para referenciar `src/` existente

**Arquivos afetados:**

```
chat-backend/pom.xml                          ← Reescrever como POM pai (packaging: pom)
chat-backend/core/pom.xml                     ← Criar
chat-backend/core/src/main/java/.../ChatApplication.java  ← Criar (inicialmente vazio/delega)
chat-backend/api/pom.xml                      ← Criar
chat-backend/ai-agent/pom.xml                 ← Criar (módulo Maven real)
chat-backend/src/                             ← Permanece intacto (código legado)
```

**Dependências:** Sprint 1A

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| `build-helper-maven-plugin` no core conflita com POM pai | Média | Alto | Testar em branch separada, manter fallback |
| Package scan do Spring não encontra beans no diretório `src/` | Alta | Alto | Configurar `@SpringBootApplication(scanBasePackages)` explicitamente |
| `spring-boot-maven-plugin` no core tenta repackaging duplicado | Média | Alto | Garantir que apenas o módulo `core` tem o plugin configurado |
| ai-agent usa classes do `src/` que não estão mais no mesmo classpath | Alta | Crítico | Mapear dependências do ai-agent primeiro |

**Estimativa:** A definir

**Critério de aceite:**
- [ ] `mvn clean compile` passa em 0 falhas
- [ ] `mvn clean test` passa todos os 17 testes existentes
- [ ] `mvn clean package -DskipTests` gera `core/target/*.jar` executável
- [ ] `docker-compose up --build` com build atual (Sprint 1C ajustará) — pode falhar se Dockerfile não atualizado
- [ ] Nenhum código movido de `src/` para os módulos
- [ ] Backend compila e roda (validar com `java -jar core/target/*.jar`)

**Checklist:**
- [ ] Decidir groupId (manter `com.project.chat` ou `com.project.chat.core` para módulo core)
- [ ] Configurar `<parent>` no `pom.xml` de cada módulo filho
- [ ] Verificar se há dependências circulares entre `core` → `api` → `ai-agent`
- [ ] Testar `mvn clean install -pl core -am` (build apenas do core + dependências)
- [ ] Verificar que `ChatApplication.java` no core não conflita com `ChatApplication.java` em `src/`

**Resultado esperado:** Projeto compila em estrutura multi-módulo. Código legado permanece intacto em `src/`. `ai-agent` é um módulo Maven real. Dockerfile ainda não funciona (será ajustado na Sprint 1C).

---

### Sprint 1C — Docker + Build + CI

**Duração:** A definir

**Objetivo:** Atualizar Dockerfile, processo de build e (se necessário) CI para funcionar com a estrutura multi-módulo criada na Sprint 1B. Validar que tudo compila e roda em containers.

**Escopo:**

1. Atualizar Dockerfile:
   - Copiar POM pai + todos os módulos (`core/`, `api/`, `ai-agent/`)
   - Executar `mvn clean package -pl core -am -DskipTests`
   - Copiar `core/target/*.jar` para a imagem final
   - Remover referências antigas a `src/` e `ai-agent/` como diretórios plano
2. Atualizar `docker-compose.yml` se necessário:
   - Verificar se o contexto de build (`context: ./chat-backend`) ainda é válido
   - Ajustar caminhos se necessário
3. Atualizar processo de build local (scripts, Makefile, etc. se existirem)
4. Atualizar CI (GitHub Actions, etc.) se necessário:
   - Atualizar comando de build para multi-módulo
   - Garantir que `mvnw` é usado (não `mvn` global)
5. Validar:
   - `docker-compose build` funciona
   - `docker-compose up` funciona
   - Endpoints respondem como antes

**Restrições:**
- ❌ Não alterar código Java
- ❌ Não alterar comportamento de endpoints
- ❌ Não adicionar novos serviços ao docker-compose
- ✅ Dockerfile pode ser substancialmente modificado

**Arquivos afetados:**

```
chat-backend/Dockerfile                      ← Modificar (multi-módulo)
chat-backend/docker-compose.yml              ← Possível ajuste
chat-backend/.github/workflows/*.yml         ← Possível ajuste (se existir)
```

**Dependências:** Sprint 1B (estrutura multi-módulo)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Dockerfile novo quebra por caminho incorreto | Alta | Crítico | Testar com `docker compose build` local antes de commit |
| Cache de camadas Docker perdido (build mais lento) | Alta | Médio | Organizar COPY para maximizar cache (pom.xml primeiro) |
| `docker-compose` contexto de build precisa mudar | Média | Médio | Se necessário, ajustar `context` no docker-compose.yml |

**Estimativa:** A definir

**Critério de aceite:**
- [ ] `docker compose build` passa sem erros
- [ ] `docker compose up` inicia todos os 4 serviços (postgres, ollama, n8n, backend)
- [ ] `GET /api/health` retorna 200
- [ ] `POST /api/chat/message` retorna resposta (fluxo feliz)
- [ ] `mvn clean package -pl core -am -DskipTests` funciona localmente
- [ ] Nenhum endpoint alterado (mesmas respostas, mesmos códigos HTTP)
- [ ] Nenhum arquivo .java alterado
- [ ] CI pipeline (se existir) passa com novos comandos de build

---

### Sprint 2 — Chat Domain: Entidades Puras

**Duração:** 1 semana (dias 11-15)

**Objetivo:** Mover `Session`, `Conversation`, `Message`, `Attachment` para `chat-core` como entidades de domínio puras (sem JPA). Criar contrapartes JPA em infrastructure.

**Escopo:**

1. Copiar entidades para `chat-core`, removendo anotações JPA:
   - `Session.java` → domain entity pura
   - `Conversation.java` → domain entity pura
   - `Message.java` → domain entity pura
   - `Attachment.java` → domain entity pura
   - `MessageRole.java` → value object
   - `DocumentStatus.java` → value object (mover para document-engine depois)
2. Adicionar comportamento nas entidades:
   - `Session.isExpired()` → encapsular lógica de expiração
   - `Conversation.addMessage()` → garantir invariantes (role alternado?)
   - `Conversation.updateTitle()` → regra de tamanho máximo
3. Criar entidades JPA em `infrastructure`:
   - `SessionJpaEntity`, `ConversationJpaEntity`, `MessageJpaEntity`, `AttachmentJpaEntity`
4. Criar mappers JPA ↔ Domain em infrastructure
5. Marcar entidades antigas como `@Deprecated`
6. Atualizar repositórios JPA existentes para usar novas entidades

**Arquivos afetados:**

```
Criar:
  chat-backend/chat-core/src/main/java/com/project/chat/core/
    ├── entity/Session.java              ← Nova (pura)
    ├── entity/Conversation.java         ← Nova (pura)
    ├── entity/Message.java              ← Nova (pura)
    ├── entity/Attachment.java           ← Nova (pura)
    ├── vo/SessionId.java                ← Novo
    ├── vo/MessageRole.java              ← Novo (enum puro)
    └── vo/ConversationTitle.java        ← Novo (value object)

  chat-backend/infrastructure/src/main/java/.../persistence/
    ├── entity/SessionJpaEntity.java     ← Nova (JPA)
    ├── entity/ConversationJpaEntity.java
    ├── entity/MessageJpaEntity.java
    ├── entity/AttachmentJpaEntity.java
    ├── mapper/SessionMapper.java        ← Domain ↔ JPA
    ├── mapper/ConversationMapper.java
    ├── mapper/MessageMapper.java
    └── mapper/AttachmentMapper.java

Modificar:
  chat-backend/src/main/java/.../entity/*.java  ← @Deprecated
```

**Dependências:** Sprint 1 (portas definidas)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| JPA annotations ausentes quebram queries | Média | Crítico | Testar todas as queries nativas + JPQL |
| Lazy loading não funciona com entidades puras | Alta | Alto | Usar DTOs projetados ou `EntityGraph` nas queries |
| Conversão JPA→Domain duplica código | Alta | Médio | Usar MapStruct ou record mapping |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] Entidades de domínio em chat-core sem import JPA/Hibernate
- [ ] Entidades JPA em infrastructure mapeiam corretamente
- [ ] Migração Flyway continua funcionando
- [ ] Queries nativas (pgvector) ainda retornam resultados
- [ ] CRUD básico de sessão, conversa e mensagem funciona via REST

**Checklist:**
- [ ] Decidir se entidades JPA ficam em `infrastructure` ou `core` com @Entity
- [ ] Configurar `@SecondaryTable` se necessário para compatibilidade
- [ ] Testar inserção de mensagem com attachment via REST
- [ ] Verificar que índices do banco continuam sendo criados

**Resultado esperado:** Domínio de chat independente de JPA, com entidades puras e comportamento encapsulado. Código legado ainda funciona via @Deprecated.

---

### Sprint 3 — Use Cases: Chat Messaging

**Duração:** 1 semana (dias 16-20)

**Objetivo:** Criar os primeiros use cases em `application` e extrair lógica duplicada de validação de sessão e criação de conversa.

**Escopo:**

1. Criar `application` module com:
   - `SendMessageUseCase`
   - `UploadAndAskUseCase`
   - `GetHistoryUseCase`
   - `GetConversationUseCase`
2. Extrair `SessionValidator` para `chat-core` domain service:
   - Valida sessão existe
   - Valida sessão não expirada
   - Atualiza `lastActivity`
3. Extrair `ConversationManager` para `chat-core` domain service:
   - Cria nova conversa com título derivado
   - Reusa conversa existente
   - Garante ownership session-conversation
4. Adaptar `RagChatService` para delegar para use cases
5. Adaptar `SimulatedChatService` para usar `SessionValidator` + `ConversationManager`

**Arquivos afetados:**

```
Criar:
  chat-backend/application/src/main/java/.../chat/
    ├── SendMessageUseCase.java
    ├── UploadAndAskUseCase.java
    ├── GetHistoryUseCase.java
    └── GetConversationUseCase.java

  chat-backend/chat-core/src/main/java/.../core/service/
    ├── SessionValidator.java             ← Novo (domain service)
    └── ConversationManager.java          ← Novo (domain service)

Modificar:
  chat-backend/src/main/java/.../service/RagChatService.java
  chat-backend/src/main/java/.../service/SimulatedChatService.java
  chat-backend/src/main/java/.../service/ConversationService.java
```

**Dependências:** Sprint 2 (entidades puras)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| @Transactional mal posicionado quebra atomicidade | Média | Alto | Use case assume responsabilidade da transação |
| Extrair SessionValidator quebra regressão | Média | Alto | Testar fluxos: sessão válida, expirada, inexistente |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `SessionValidator` usado por RagChatService e SimulatedChatService
- [ ] `ConversationManager` usado por ambos
- [ ] `SendMessageUseCase.id` implementado com rollback em caso de erro
- [ ] `POST /api/chat/message` responde igual antes da refatoração
- [ ] `POST /api/chat/upload-and-ask` responde igual
- [ ] `GET /api/chat/history/{sessionId}` responde igual

**Checklist:**
- [ ] Validar @Transactional (colocar no use case, não no service antigo)
- [ ] Testar sessão expirada → 409 Conflict
- [ ] Testar sessão inexistente → 404 Not Found
- [ ] Testar criação de conversa → 201 com conversationId
- [ ] Testar reuso de conversa → mensagem adicionada à conversa existente

**Resultado esperado:** Duplicação de lógica de sessão/conversa entre RagChatService e SimulatedChatService eliminada.

---

### Sprint 4 — MessageMapper sem Regra de Banco

**Duração:** 1 semana (dias 21-25)

**Objetivo:** Remover regra de negócio de `MessageMapper.toEntity()` e limpar acoplamentos nos mappers.

**Escopo:**

1. Extrair lógica de vinculação de attachment para `AttachmentService` em `chat-core`
2. Refatorar `MessageMapper.toEntity()`:
   - Remove `attachmentRepository.findById()`
   - Recebe `Attachment` já resolvido como parâmetro
3. Refatorar `ConversationMapper`:
   - Remove dependência de `MessageMapper`
4. Criar `AttachmentResolver` como domain service
5. Atualizar `RagChatService` e `SimulatedChatService` para usar novo fluxo

**Arquivos afetados:**

```
Modificar:
  chat-backend/src/main/java/.../mapper/MessageMapper.java
  chat-backend/src/main/java/.../mapper/ConversationMapper.java

Criar:
  chat-backend/chat-core/src/main/java/.../core/service/
    └── AttachmentResolver.java

Remover (depois da migração):
  chat-backend/src/main/java/.../mapper/ → manter @Deprecated por 1 sprint
```

**Dependências:** Sprint 3 (use cases)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Quebra de API em mappers usados por múltiplos services | Média | Alto | Compilador detecta, mudanças são locais |

**Estimativa:** 3 dias

**Critério de aceite:**
- [ ] `MessageMapper.toEntity()` não chama mais `attachmentRepository`
- [ ] `ConversationMapper` não injeta mais `MessageMapper`
- [ ] Upload + mensagem com attachment funciona
- [ ] Nenhum mapper contém lógica de banco

**Resultado esperado:** Mappers viram verdadeiros conversores (SRP respeitado).

---

### Sprint 5 — Document Engine

**Duração:** 1 semana (dias 26-30)

**Objetivo:** Extrair `document-engine` com entidades de documento puras e pipeline de ingestão unificado.

**Escopo:**

1. Criar módulo `document-engine` com:
   - Entidades: `Document`, `DocumentChunk`, `DocumentMetadata`
   - Value Objects: `DocumentStatus`, `SourceType`, `ChunkIndex`
   - Port: `DocumentRepository`, `DocumentChunkRepository`, `DocumentParser`
2. Mover `ChunkService` como domain service puro
3. Criar `IngestionPipeline` domain service:
   - Unifica o padrão chunk → embed → save que está em 5 lugares
4. Extrair `ParserFactory` como port (interface)
5. Manter `DocumentIngestionService` como fachada @Deprecated

**Arquivos afetados:**

```
Criar:
  chat-backend/document-engine/pom.xml
  chat-backend/document-engine/src/main/java/.../document/
    ├── entity/Document.java
    ├── entity/DocumentChunk.java
    ├── entity/DocumentMetadata.java
    ├── vo/DocumentStatus.java
    ├── vo/SourceType.java
    ├── port/DocumentRepository.java
    ├── port/DocumentChunkRepository.java
    ├── port/DocumentParser.java
    ├── service/ChunkService.java          ← Movido
    └── service/IngestionPipeline.java     ← Novo

Modificar:
  chat-backend/src/main/java/.../service/DocumentIngestionService.java ← @Deprecated
  chat-backend/src/main/java/.../service/ChunkService.java             ← @Deprecated
  chat-backend/src/main/java/.../entity/Document.java                  ← @Deprecated
```

**Dependências:** Sprint 2 (entidades puras), Sprint 3 (use cases)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Pipeline replicado em 5 lugares → 5 chances de regressão | Alta | Alto | Testar ingestão de cada tipo de arquivo |
| ChunkService usado em múltiplos contextos | Alta | Médio | Criar interface e manter compatibilidade |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `IngestionPipeline` usado por todos os fluxos de ingestão
- [ ] ChunkService movido para document-engine sem alteração de API
- [ ] `POST /api/documents/ingest` com PDF, TXT, HTML, DOCX funciona
- [ ] `POST /api/documents/ingest/url` funciona
- [ ] Número de chunks gerados é idêntico ao anterior
- [ ] Totais de documentos nos 5 lugares coincidem

**Checklist:**
- [ ] Comparar output do pipeline antigo vs novo (diff dos chunks)
- [ ] Testar com arquivo de 100KB (deve gerar ~128 chunks de 800 chars)
- [ ] Verificar webhook é chamado com mesmos parâmetros

**Resultado esperado:** Pipeline de ingestão único, testável e reutilizável. Duplicação eliminada.

---

### Sprint 6 — RAG Engine

**Duração:** 1 semana (dias 31-35)

**Objetivo:** Extrair `rag-engine` com orquestrador de RAG e portas de retrieval. Eliminar duplicação de busca semântica.

**Escopo:**

1. Criar módulo `rag-engine`:
   - `RagOrchestrator` (orquestra embed → retrieve → prompt)
   - `RetrievalService` port
   - `RerankingService` port (opcional)
2. Criar `RetrievalAdapter` em infrastructure (pgvector)
3. Unificar busca semântica:
   - Remover `DocumentIngestionService.searchSimilar()`
   - Remover `DocumentQueryService.computeSimilarity()` (inline)
   - Manter apenas `RagOrchestrator.retrieve()`
4. Criar `NoopRetrievalAdapter` para profile dev

**Arquivos afetados:**

```
Criar:
  chat-backend/rag-engine/pom.xml
  chat-backend/rag-engine/src/main/java/.../rag/
    ├── RagOrchestrator.java
    ├── RagContext.java
    ├── RetrievalResult.java
    └── port/RetrievalService.java

  chat-backend/infrastructure/src/main/java/.../rag/
    └── PgVectorRetrievalAdapter.java
    └── NoopRetrievalAdapter.java

Modificar:
  chat-backend/src/main/java/.../service/VectorRetrievalService.java  ← @Deprecated
  chat-backend/src/main/java/.../service/NoopRetrievalService.java    ← @Deprecated
  chat-backend/src/main/java/.../service/DocumentQueryService.java    ← remover searchSimilar
```

**Dependências:** Sprint 5 (document-engine)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Query nativa pgvector precisa ser ajustada | Baixa | Alto | Testar com vetores reais (768 dim) |
| Reranking adiciona latência | Média | Baixo | Torna-se opcional, ativado por config |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `RagOrchestrator.retrieve(query, topK)` retorna chunks
- [ ] `RagOrchestrator.execute(question)` → RAG completo
- [ ] Busca semântica via REST (`POST /api/documents/search`) funciona
- [ ] Perfil `dev` usa `NoopRetrievalAdapter`
- [ ] Perfil `rag` usa `PgVectorRetrievalAdapter`
- [ ] Número de resultados com e sem reranking é consistente

**Resultado esperado:** Módulo RAG autocontido, retrieval desacoplado, busca semântica unificada.

---

### Sprint 7 — LLM Engine

**Duração:** 1 semana (dias 36-40)

**Objetivo:** Extrair `llm-engine` com portas para Chat, Embedding e Vision. Criar adaptadores Ollama.

**Escopo:**

1. Criar módulo `llm-engine`:
   - `ChatLlmService` (generate, generateStream)
   - `EmbeddingService` (embed, embedAll)
   - `VisionService` (describeImage)
   - Value Objects: `LlmConfig`, `ModelType`, `Prompt`, `Response`
2. Criar adaptadores em infrastructure:
   - `OllamaChatAdapter`
   - `OllamaEmbeddingAdapter`
   - `OllamaVisionAdapter`
3. Mover `PromptBuilder` para `llm-engine` (sem conhecimento Marvel)
4. Criar `MarvelPromptEnricher` em marvel-plugin (com conhecimento específico)
5. Remover código duplicado do módulo `ai-agent`

**Arquivos afetados:**

```
Criar:
  chat-backend/llm-engine/pom.xml
  chat-backend/llm-engine/src/main/java/.../llm/
    ├── port/ChatLlmService.java
    ├── port/EmbeddingService.java
    ├── port/VisionService.java
    ├── vo/LlmConfig.java
    ├── vo/Prompt.java
    ├── vo/Response.java
    └── service/PromptBuilder.java            ← Movido (genérico)

  chat-backend/infrastructure/src/main/java/.../llm/
    ├── OllamaChatAdapter.java
    ├── OllamaEmbeddingAdapter.java
    └── OllamaVisionAdapter.java

Remover (após migração):
  chat-backend/src/main/java/.../service/OllamaChatService.java
  chat-backend/src/main/java/.../service/OllamaEmbeddingService.java
  chat-backend/src/main/java/.../service/OllamaVisionService.java
  chat-backend/ai-agent/                      ← Incorporar ou remover
```

**Dependências:** Sprint 5 (document-engine), Sprint 6 (rag-engine)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Streaming SSE precisa ser adaptado | Média | Alto | Manter SseEmitter na API, adaptar internamente |
| OllamaClient duplicado (ai-agent vs main) | Alta | Médio | Unificar em OllamaChatAdapter |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `OllamaChatAdapter.generate(prompt)` retorna resposta
- [ ] `OllamaChatAdapter.generateStream(prompt, callback)` emite tokens
- [ ] `OllamaEmbeddingAdapter.embed(text)` retorna float[768]
- [ ] `OllamaVisionAdapter.describeImage(bytes)` retorna descrição
- [ ] `POST /api/chat/message/stream` retorna SSE com tokens
- [ ] Fallback `/api/embeddings` funciona (Ollama versão antiga)

**Checklist:**
- [ ] Testar com Ollama desligado → resposta amigável
- [ ] Testar timeout → LlmServiceException
- [ ] Testar streaming com 500 tokens
- [ ] Verificar que `OLLAMA_KEEP_ALIVE=24h` reduz latência

**Resultado esperado:** LLM Engine independente de tecnologia. Trocar Ollama por OpenAI = novo adapter.

---

### Sprint 8 — Vision Engine

**Duração:** 1 semana (dias 41-45)

**Objetivo:** Extrair `vision-engine` com OCR e visão computacional. Fallback automático OCR → Vision LLM.

**Escopo:**

1. Criar módulo `vision-engine`:
   - `OcrService` port
   - `VisionService` port (já existe em llm-engine, re-exportar)
   - `ImageAnalysisOrchestrator` (tenta OCR, fallback para Vision LLM)
2. Criar `TesseractOcrAdapter` em infrastructure
3. Criar `ImageTypeDetector` em vision-engine
4. Integrar com `IngestionPipeline` (document-engine)
5. Remover `ImageOcrParser` antigo

**Arquivos afetados:**

```
Criar:
  chat-backend/vision-engine/pom.xml
  chat-backend/vision-engine/src/main/java/.../vision/
    ├── port/OcrService.java
    ├── service/ImageAnalysisOrchestrator.java
    └── service/ImageTypeDetector.java

  chat-backend/infrastructure/src/main/java/.../vision/
    └── TesseractOcrAdapter.java

Modificar:
  chat-backend/document-engine/src/main/java/.../document/
    └── service/IngestionPipeline.java  ← usar ImageAnalysisOrchestrator para imagens

Remover:
  chat-backend/src/main/java/.../parser/ImageOcrParser.java
```

**Dependências:** Sprint 7 (llm-engine), Sprint 5 (document-engine)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Tesseract não instalado no container Docker | Alta | Alto | Adicionar ao Dockerfile + fallback silencioso |
| OCR em imagem grande causa OOM | Média | Médio | Limitar tamanho da imagem (max 10MB) |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `ImageAnalysisOrchestrator.analyze(image)` retorna texto
- [ ] OCR funciona para PNG, JPG, BMP, TIFF
- [ ] Fallback Vision LLM é acionado quando OCR retorna vazio
- [ ] Ingestão de imagem via REST retorna chunks
- [ ] Upload-and-ask com imagem funciona (OCR → RAG → resposta)
- [ ] Tesseract instalado no container (Dockerfile atualizado)

**Resultado esperado:** Módulo de visão independente, testável, com fallback automático.

---

### Sprint 9 — Knowledge Engine

**Duração:** 1 semana (dias 46-50)

**Objetivo:** Criar `knowledge-engine` para gerenciar fontes de conhecimento com plugins.

**Escopo:**

1. Criar módulo `knowledge-engine`:
   - `KnowledgeSourceProvider` port
   - `KnowledgeIngestionOrchestrator`
   - `KnowledgeSearchService`
   - Domain Events: `SourceSyncStartedEvent`, `SourceSyncCompletedEvent`
2. Mover lógica Marvel de `MarvelIngestionController` para o orchestrator
3. `MarvelIngestionController` perde toda regra de negócio (vira fachada)
4. Criar integração com `IngestionPipeline` para salvar documentos

**Arquivos afetados:**

```
Criar:
  chat-backend/knowledge-engine/pom.xml
  chat-backend/knowledge-engine/src/main/java/.../knowledge/
    ├── port/KnowledgeSourceProvider.java
    ├── service/KnowledgeIngestionOrchestrator.java
    ├── service/KnowledgeSearchService.java
    └── event/KnowledgeSyncEvent.java

Modificar:
  chat-backend/src/main/java/.../controller/MarvelIngestionController.java
    └── remover toda lógica de negócio, delegar para use case
```

**Dependências:** Sprint 5 (document-engine), Sprint 6 (rag-engine)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| MarvelIngestionController tem @Transactional com lógica | Alta | Médio | Mover @Transactional para o use case |
| Múltiplas fontes concorrentes podem conflitar | Baixa | Médio | Usar locks otimistas no banco |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `KnowledgeIngestionOrchestrator` coordena ingestão de fontes
- [ ] `MarvelIngestionController` apenas delega (máximo 3 linhas por método)
- [ ] `POST /api/marvel/ingest/all` funciona igual
- [ ] `POST /api/marvel/ingest/fandom` funciona igual
- [ ] Nenhuma regra de negócio no controller

**Resultado esperado:** Knowledge engine extensível. Para adicionar nova fonte (ex: DC Comics), basta implementar `KnowledgeSourceProvider`.

---

### Sprint 10 — Marvel Plugin

**Duração:** 1 semana (dias 51-55)

**Objetivo:** Extrair conhecimento Marvel para plugin destacável. Externalizar prompt knowledge base.

**Escopo:**

1. Criar módulo `marvel-plugin`:
   - `MarvelApiProvider` (implementa KnowledgeSourceProvider)
   - `FandomScraperProvider`
   - `WikipediaApiProvider`
   - `MarvelPromptEnricher` (knowledge base do MCU)
2. Externalizar conhecimento do MCU:
   - Mover ~400 linhas de texto de `MarvelPromptBuilder` para `marvel-knowledge.yml`
   - Carregar via `@ConfigurationProperties`
   - Possibilitar atualização sem recompilar
3. Remover dependência de `ai-agent` module
4. Integrar `MarvelPromptEnricher` com `PromptBuilder` em llm-engine

**Arquivos afetados:**

```
Criar:
  chat-backend/marvel-plugin/pom.xml
  chat-backend/marvel-plugin/src/main/java/.../marvel/
    ├── MarvelApiProvider.java
    ├── FandomScraperProvider.java
    ├── WikipediaApiProvider.java
    └── MarvelPromptEnricher.java
  chat-backend/marvel-plugin/src/main/resources/
    └── marvel-knowledge.yml

Remover:
  chat-backend/ai-agent/                         ← Todo o módulo
  chat-backend/src/main/java/.../ai/             ← Mover/remover

Modificar:
  chat-backend/llm-engine/src/main/java/.../llm/service/PromptBuilder.java
    └── injetar MarvelPromptEnricher opcionalmente
  chat-backend/pom.xml                           ← Remover build-helper-maven-plugin
```

**Dependências:** Sprint 9 (knowledge-engine)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| marvel-plugin opcional pode quebrar imports | Média | Alto | Usar `@Autowired(required=false)` + Optional |
| Conhecimento MCU externalizado perde versionamento | Baixa | Médio | Manter no Git, versão no nome do arquivo |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] `marvel-knowledge.yml` carregado via @ConfigurationProperties
- [ ] `MarvelPromptEnricher` injetado opcionalmente
- [ ] Sistema funciona SEM marvel-plugin no classpath
- [ ] Sistema funciona COM marvel-plugin (conhecimento MCU ativo)
- [ ] `build-helper-maven-plugin` removido
- [ ] Todos os endpoints Marvel continuam funcionando

**Checklist:**
- [ ] Extrair constantes MARVEL_SYSTEM_PROMPT e MARVEL_KNOWLEDGE_BASE
- [ ] Validar que YAML tem encoding UTF-8
- [ ] Testar marvel-plugin ausente → chat funciona sem conhecimento MCU
- [ ] Testar marvel-plugin presente → respostas enriquecidas com MCU

**Resultado esperado:** Plugin Marvel destacável. Conhecimento MCU externalizado em YAML editável sem recompilar. Módulo ai-agent eliminado.

---

### Sprint 11 — Resiliência + Observabilidade

**Duração:** 1 semana (dias 56-60)

**Objetivo:** Tornar o sistema pronto para produção com resiliência, métricas e gerenciamento de recursos.

**Escopo:**

1. **Gerenciamento de Threads:**
   - Substituir `Executors.newFixedThreadPool(10)` por `ThreadPoolTaskExecutor`
   - Configurar pool names, queue capacity, rejection policy
   - Adicionar `@PreDestroy` para shutdown graceful
2. **Webhook Assíncrono:**
   - Publicar `DocumentIngestedEvent`
   - `WebhookEventHandler` escuta evento em thread separada
   - Retry com exponential backoff
3. **Circuit Breaker (Ollama):**
   - Adicionar `resilience4j-spring-boot3`
   - `@CircuitBreaker(name = "ollamaChat", fallbackMethod = "chatFallback")`
   - `@CircuitBreaker(name = "ollamaEmbedding", fallbackMethod = "embeddingFallback")`
   - `@TimeLimiter(name = "ollama")`
4. **Rate Limiter (Embedding):**
   - Semáforo com máximo de 2 chamadas simultâneas
   - Configurável via `application.yml`
5. **Métricas:**
   - Micrometer + Actuator (já incluído no Spring Boot)
   - Métricas customizadas: `rag.query.time`, `rag.chunks.retrieved`, `ingestion.documents`, `llm.tokens`
   - Health indicators: `OllamaHealthIndicator`, `PgVectorHealthIndicator`, `N8nHealthIndicator`
6. **Paginação:**
   - `DocumentRepository.findAll()` → `findAll(Pageable)`
   - Endpoints de listagem com `page` e `size`
7. **TaskService persistente:**
   - Substituir `ConcurrentHashMap` por tabela `async_tasks` no banco
   - `TaskRepository` JPA
   - Scheduler para limpar tasks antigas
8. **Sessão expirada:**
   - `@Scheduled(cron = "0 0 */6 * * *")` → marcar sessões expiradas
   - `SessionRepository.findByExpiredFalseAndLastActivityBefore()`

**Arquivos afetados:**

```
Modificar:
  chat-backend/infrastructure/src/main/java/.../llm/
    ├── OllamaChatAdapter.java                 ← @CircuitBreaker + @TimeLimiter
    └── OllamaEmbeddingAdapter.java            ← @CircuitBreaker + RateLimiter

  chat-backend/infrastructure/src/main/java/.../webhook/
    └── WebhookEventHandler.java               ← Novo (event-driven)

  chat-backend/application/src/main/java/.../chat/
    └── SendMessageUseCase.java                ← métricas

  chat-backend/application/src/main/java/.../document/
    └── IngestDocumentUseCase.java             ← publicar evento

Criar:
  chat-backend/infrastructure/src/main/java/.../task/
    ├── entity/AsyncTaskJpaEntity.java
    ├── repository/AsyncTaskRepository.java
    └── service/PersistentTaskService.java

  chat-backend/infrastructure/src/main/java/.../health/
    ├── OllamaHealthIndicator.java
    ├── PgVectorHealthIndicator.java
    └── N8nHealthIndicator.java

  chat-backend/core/src/main/java/.../config/
    ├── ThreadPoolConfig.java
    ├── Resilience4jConfig.java
    └── SchedulingConfig.java
```

**Dependências:** Sprint 0-10 (todos os módulos existentes)

**Riscos:**
| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Circuit breaker falso positivo durante inicialização | Alta | Médio | Configurar `ringBufferSizeInClosedState` adequado |
| Thread pool configurado incorretamente causa starvation | Média | Alto | Testes de carga com k6 ou Gatling |
| Event bus síncrono (Spring default) anula benefício | Média | Médio | Verificar `@Async` + `@EnableAsync` |

**Estimativa:** 5 dias

**Critério de aceite:**
- [ ] Thread pools têm shutdown graceful (nenhuma thread órfã após restart)
- [ ] Webhook não bloqueia resposta de ingestão (resposta < 200ms)
- [ ] Circuit breaker abre quando Ollama está offline (resposta fallback)
- [ ] Rate limiter de embedding respeita máximo configurado
- [ ] `GET /api/health` mostra status individual de DB, Ollama, n8n
- [ ] `GET /api/documents?page=0&size=10` retorna paginado
- [ ] Tasks async sobrevivem a restart (persistidas no banco)
- [ ] Sessões expiradas são limpas a cada 6 horas
- [ ] Métricas disponíveis em `/actuator/metrics`

**Checklist de não-regressão:**
- [ ] `POST /api/chat/message` → resposta em < 5s (RAG incluso)
- [ ] `POST /api/documents/ingest` (PDF 1MB) → resposta em < 10s
- [ ] `POST /api/chat/message/stream` → primeiro token em < 3s
- [ ] `POST /api/marvel/ingest/fandom` → executa sem erros

**Resultado esperado:** Sistema preparado para produção: resiliente, observável, sem vazamento de recursos, com fallbacks claros.

---

## Backlog

Prioridade: P0 (imediata), P1 (próxima release), P2 (futuro), P3 (nice-to-have)

| ID | Item | Prioridade | Módulo | Sprint |
|---|---|---|---|---|
| B01 | OpenAPI/Swagger (springdoc-openapi) | P1 | api | Pós-v3.0 |
| B02 | Testes de integração com Testcontainers | P1 | infrastructure | Sprint 11+ |
| B03 | CI/CD pipeline (GitHub Actions) | P0 | core | Sprint 0 |
| B04 | Cache de embeddings frequentes (Caffeine) | P2 | llm-engine | Futuro |
| B05 | Dashboard administrativo (React/Vue) | P3 | api + frontend | Futuro |
| B06 | Suporte a mais formatos de arquivo (EPUB, RTF) | P2 | document-engine | Futuro |
| B07 | WebSocket em vez de SSE para streaming bidirecional | P2 | api | Futuro |
| B08 | Auditoria de queries (log de perguntas + respostas) | P2 | application | Futuro |
| B09 | Multilíngue (suporte a perguntas em inglês) | P2 | llm-engine | Futuro |
| B10 | Personalização por usuário (histórico cross-session) | P3 | chat-core | Futuro |
| B11 | Suporte a modelos OpenAI (gpt-4) via adapter | P2 | llm-engine | Futuro |
| B12 | Suporte a Anthropic Claude via adapter | P2 | llm-engine | Futuro |
| B13 | Dashboard de métricas (Grafana) | P2 | infra | Futuro |
| B14 | Alertas (Prometheus + Alertmanager) | P2 | infra | Futuro |
| B15 | Backup automático do banco | P1 | infra | Sprint 11 |
| B16 | Logs estruturados (JSON) para ELK/Loki | P2 | core | Futuro |
| B17 | Feature flags (split de Marvel vs Genérico) | P3 | application | Futuro |
| B18 | Testes de carga (k6 script) | P1 | core | Sprint 11 |
| B19 | Documentação de API em markdown auto-gerada | P2 | api | Futuro |
| B20 | Modo offline (usar cache local sem Ollama) | P3 | llm-engine | Futuro |

---

## Bugs Conhecidos

| ID | Bug | Módulo | Severidade | Status | Observação |
|---|---|---|---|---|---|
| K01 | ExecutorService nunca é shutdown (vazamento de threads) | RagChatService | CRÍTICO | Aberto | `Executors.newFixedThreadPool(10)` sem `@PreDestroy` |
| K02 | Webhook síncrono bloqueia resposta de ingestão | DocumentIngestionService | ALTO | Aberto | `webhookService.notify()` dentro do método principal |
| K03 | TaskService perde tasks em restart (ConcurrentHashMap) | TaskService | ALTO | Aberto | Dados em memória não persistem |
| K04 | `DocumentRepository.findAll()` sem paginação | DocumentQueryService | ALTO | Aberto | Pode causar OOM com muitos documentos |
| K05 | Session expiration nunca é executada (sem @Scheduled) | SessionService | MÉDIO | Aberto | `findByExpiredFalseAndLastActivityBefore()` existe mas não é chamado |
| K06 | `ImageOcrParser` engole exceções silenciosamente | ImageOcrParser | MÉDIO | Aberto | TesseractException retorna "" sem feedback |
| K07 | MAX_CHARS=8000 hardcoded em OllamaEmbeddingService | OllamaEmbeddingService | BAIXO | Aberto | Deveria ser configurável |
| K08 | IVFFlat index duplicado (DatabaseInitializer + migration V1) | config / db | BAIXO | Aberto | Um deles sempre falha silenciosamente |
| K09 | `PdfTextExtractor` deprecated mas ainda compilado | util | BAIXO | Aberto | Código morto |
| K10 | `MessageMapper.toEntity()` faz regra de banco (attachmentRepository.findById) | MessageMapper | ALTO | Aberto | Viola SRP |
| K11 | `application-rag.yml` e `application-prod.yml` quase idênticos | config | MÉDIO | Aberto | Duplicação de configuração |
| K12 | `DocumentController.SUPPORTED_INGESTION_TYPES` duplica `FileUtils.ALLOWED_MIME_TYPES` | DocumentController | BAIXO | Aberto | Constantes espalhadas |

---

## Melhorias Futuras

### Curto Prazo (pós-v3.0)

| Melhoria | Motivação | Esforço |
|---|---|---|
| Adicionar `@Async` no webhook | Liberar resposta de ingestão imediatamente | 2h |
| Configurar `ThreadPoolTaskExecutor` gerenciado | Eliminar vazamento de threads | 2h |
| Adicionar paginação em listDocuments | Evitar OOM | 1h |
| Externalizar MAX_CHARS | Configurável sem recompilar | 30min |
| Adicionar `@Scheduled` para expirar sessões | Limpeza automática | 1h |

### Médio Prazo (v3.1)

| Melhoria | Motivação | Esforço |
|---|---|---|
| OpenAPI/Swagger (springdoc-openapi) | Documentação automática da API | 1 dia |
| Testes de integração com Testcontainers | Confiança nas queries pgvector | 3 dias |
| Cache de embeddings (Caffeine) | Reduzir chamadas ao Ollama | 2 dias |
| CI/CD com GitHub Actions | Automação de build + deploy | 2 dias |

### Longo Prazo (v3.2+)

| Melhoria | Motivação |
|---|---|
| Frontend React/Vue | Interface gráfica para o chat |
| WebSocket bidirecional | Streaming mais eficiente que SSE |
| Suporte OpenAI/Anthropic | Mais opções de LLM |
| Modo offline | Funcionar sem Ollama (fallback para regras) |
| Administração de documentos | Gerenciar knowledge base via UI |

---

## Métricas

### Estado Atual (v2.0.0)

| Métrica | Valor | Alvo (v3.0.0) |
|---|---|---|
| Cobertura de testes | ~15% | >80% |
| Complexidade ciclomática (RagChatService) | ~50 (estimado) | <10 |
| Acoplamento (dependências por classe) | 15+ (RagChatService) | <5 |
| Quantidade de módulos Maven | 1 (+ ai-agent via plugin) | 11 |
| Quantidade de endpoints REST | 20+ | 20+ (inalterado) |
| Quantidade de testes unitários | 17 | >100 |
| Quantidade de testes de integração | 0 | >20 |
| Duplicação de código | Pipeline em 5 lugares | Zero |
| Violações SRP | Múltiplas | Zero |
| God Objects | RagChatService (500+ linhas) | Nenhum |
| Thread pools sem shutdown | 2 | Zero |
| Dependências circulares | Potenciais | Zero (verificado em CI) |

### Métricas a Coletar (pós-implementação)

```
Métricas de código:
  - Linhas por classe (alvo: <200 linhas)
  - Profundidade de herança (alvo: <3)
  - Acoplamento aferente (CA) por módulo
  - Instabilidade (I = Ce / (Ca + Ce))
  - Distância da sequência principal (D = |A + I - 1|)

Métricas de teste:
  - Cobertura de linha (Jacoco) por módulo
  - Cobertura de branch
  - Número de testes por módulo
  - Tempo de execução dos testes

Métricas de runtime:
  - P50/P95/P99 de tempo de resposta (/api/chat/message)
  - Taxa de erro do Ollama
  - Número de chunks retornados por query
  - Tempo médio de ingestão por tipo de arquivo
  - Uso de thread pool (active, queued, completed)
  - Cache hit ratio (quando implementado)
```

---

## Definition of Done

Uma Sprint é considerada concluída quando **todos** os critérios abaixo são atendidos:

### Código

- [ ] Todos os arquivos planejados foram criados/modificados
- [ ] Nenhum arquivo contém `FIXME`, `TODO`, `HACK` ou `XXX` (exceto backlog)
- [ ] `mvn clean verify` passa em 0 falhas e 0 warnings
- [ ] Nenhum teste existente foi removido ou alterado sem aprovação
- [ ] Cobertura de testes do módulo >= 80% (linhas)
- [ ] Nenhum import não utilizado (verificado pelo IDE ou Checkstyle)
- [ ] Código segue as regras do `AGENTS.md` e `FUTURE_ARCHITECTURE.md`

### Compatibilidade

- [ ] Todos os endpoints REST existentes retornam as mesmas respostas (mesmo schema JSON)
- [ ] `docker-compose up --build` sobe sem erros
- [ ] `GET /api/health` retorna 200
- [ ] `POST /api/chat/message` com mensagem simples funciona (fluxo feliz)
- [ ] Perfil `dev` funciona sem Ollama
- [ ] Perfil `rag` funciona com Ollama

### Qualidade

- [ ] Nenhuma classe ultrapassou 200 linhas (exceto arquivos de config/knowledge)
- [ ] Nenhum service tem mais de 5 dependências injetadas
- [ ] Nenhum controller contém regra de negócio (máximo 3 linhas delegando)
- [ ] Nenhum mapper contém lógica de banco
- [ ] Nenhum adapter vaza exceção de tecnologia

### Arquitetura

- [ ] Dependências entre módulos seguem a matriz definida em FUTURE_ARCHITECTURE.md
- [ ] Nenhum ciclo de dependência entre módulos
- [ ] Portas estão nos módulos de domínio (não em infrastructure)
- [ ] Implementações estão em infrastructure (não em domínio)
- [ ] `pom.xml` raiz não tem dependências de módulo (cada módulo declara as suas)

### Documentação

- [ ] JavaDoc público adicionado para novas classes e métodos públicos
- [ ] README.md atualizado se necessário (mudanças de API)
- [ ] CHANGELOG.md atualizado com as mudanças da Sprint
- [ ] Se houve mudança de API: API_BACKEND_COMPLETE_SPEC.md atualizado

### Revisão

- [ ] Code review realizado por pelo menos 1 outro desenvolvedor
- [ ] Nenhum `@Deprecated` novo sem data de remoção planejada
- [ ] Arquivos `@Deprecated` da Sprint anterior foram removidos (se prazo venceu)

### Metas da Sprint

- [ ] 100% dos itens do escopo da Sprint entregues
- [ ] 0 bugs conhecidos introduzidos pela Sprint
- [ ] Backlog atualizado com novos itens descobertos

---

## Apêndice: Dependências Entre Sprints

```
Sprint 0 (Análise)
  └─► Sprint 1A (Infraestrutura Maven)
        └─► Sprint 1B (Parent POM + Módulos)
              └─► Sprint 1C (Docker + Build)
                    └─► Sprint 2 (Chat Domain)
                          └─► Sprint 3 (Use Cases)
                                └─► Sprint 4 (Mappers)
                                │
                                └─► Sprint 5 (Document Engine)
                                      └─► Sprint 6 (RAG Engine)
                                            └─► Sprint 7 (LLM Engine)
                                                  └─► Sprint 8 (Vision Engine)
                                                  │
                                                  └─► Sprint 9 (Knowledge Engine)
                                                        └─► Sprint 10 (Marvel Plugin)
                                                              │
                                                              └─► Sprint 11 (Produção)
```

**Rotas críticas:**
- Sprint 0 → 1A → 1B → 1C → 2 → 3 → 5 → 6 → 7 → 8 → 11 (11 sprints)
- Sprint 4 é paralelizável com Sprint 5 (economia de 1 sprint)
- Sprint 9 e 10 são paralelizáveis com Sprint 7 e 8 (economia de 2 sprints)

**Caminho crítico:** Sprint 0, 1A, 1B, 1C, 2, 3, 5, 6, 7, 8, 11 = 11 sprints.

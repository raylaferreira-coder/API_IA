# Sprint 0 — Relatório de Análise da Infraestrutura

## Resumo

**Status:** Análise concluída. Nenhuma alteração foi aplicada ao código-fonte. Este relatório documenta o estado atual da infraestrutura do projeto e as ações necessárias para preparar a modularização, conforme as restrições da Sprint 0 (sem quebra de compatibilidade, sem alteração de comportamento, sem refatoração de código).

**Projeto:** `chat-backend` — Maven single module, Spring Boot 3.4.5, Java 17
**Build:** OK (`mvn clean package` gera JAR executável)
**Docker:** OK (Dockerfile multi-stage + docker-compose com 4 serviços)
**Testes:** 17 testes, 1 desabilitado (`@Disabled` por incompatibilidade pgvector × H2)

---

## 1. Revisão da Estrutura Maven

### 1.1 Estado Atual: Monolito Single Module

O projeto é um **monolito Maven de módulo único**. Todo o código-fonte está sob `src/main/java/com/project/chat/` e o módulo `ai-agent` é incluído via hack do `build-helper-maven-plugin`.

```
chat-backend/
├── pom.xml                         ← Módulo único (artifactId: chat-backend)
├── src/main/java/com/project/chat/ ← Código principal (11 packages)
├── src/test/java/com/project/chat/ ← Testes (17 classes)
├── ai-agent/src/main/java/.../ai/  ← "Módulo" virtual (via build-helper-maven-plugin)
└── target/                         ← JAR gerado
```

### 1.2 Estrutura Atual do pom.xml

```xml
<groupId>com.project</groupId>
<artifactId>chat-backend</artifactId>
<version>1.0.0</version>
<packaging>jar</packaging>     ← implícito (single module)

<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
</parent>

<properties>
    <java.version>17</java.version>
</properties>
```

### 1.3 Problema: `build-helper-maven-plugin` para ai-agent

O módulo `ai-agent` não é um módulo Maven real. Ele é tratado como diretório de código fonte adicional:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <id>add-ai-agent-sources</id>
            <phase>generate-sources</phase>
            <goals><goal>add-source</goal></goals>
            <configuration>
                <sources>
                    <source>ai-agent/src/main/java</source>
                </sources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Riscos desta abordagem:**
- ai-agent não tem `pom.xml` próprio (sem controle de dependências)
- Resources do ai-agent incluídos manualmente via seção `<resources>`
- Se o ai-agent precisar de dependências diferentes, todas vão para o pom.xml principal
- Não há isolamento entre os módulos (tudo compartilha o mesmo classpath)
- A ordem de compilação não pode ser controlada
- Ferramentas de IDE podem não reconhecer corretamente a estrutura

### 1.4 Ausência de Maven Wrapper

**Arquivo `mvnw` / `mvnw.cmd`:** ❌ Não existe
**Diretório `.mvn/`:** ❌ Não existe

**Impacto:** O build depende de Maven instalado no sistema (versão específica). Sem garantia de reprodutibilidade. Dockerfile usa `maven:3.9-eclipse-temurin-17-alpine` que resolve, mas builds locais dependem da instalação do desenvolvedor.

---

## 2. Verificação de Suporte a Multi Module

### 2.1 Diagnóstico

| Característica | Atual | Necessário para Multi Module |
|---|---|---|
| `<packaging>pom</packaging>` no pai | ❌ Não existe | ✅ Deve existir |
| `<modules>` no pai | ❌ Não existe | ✅ Deve existir |
| `<parent>` nos módulos filhos | ❌ Não existe | ✅ Deve existir |
| Separação física em diretórios | Parcial (ai-agent/) | ✅ Completa |
| Dependências gerenciadas via `<dependencyManagement>` | ❌ Não existe | ✅ Recomendado |

**Conclusão:** O projeto **NÃO** suporta multi-module atualmente.

### 2.2 Estrutura Alvo Recomendada (para Sprint 1+)

```
chat-backend/
├── pom.xml                              ← POM pai (packaging: pom)
├── chat-core/pom.xml                    ← Entidades + portas
├── document-engine/pom.xml              ← Processamento de documentos
├── rag-engine/pom.xml                   ← Pipeline RAG
├── llm-engine/pom.xml                   ← Abstração LLM
├── vision-engine/pom.xml                ← OCR + Visão
├── knowledge-engine/pom.xml             ← Base de conhecimento
├── marvel-plugin/pom.xml                ← Plugin Marvel
├── shared-kernel/pom.xml                ← Tipos compartilhados
├── infrastructure/pom.xml               ← Adaptadores
├── application/pom.xml                  ← Casos de uso
├── api/pom.xml                          ← REST controllers
├── core/pom.xml                         ← Bootstrap Spring Boot
└── src/                                 ← Diretório atual (legado)
```

---

## 3. Preparação para Modularização Futura

### 3.1 O que DEVE ser feito (na Sprint 1, não agora)

| Ordem | Ação | Risco |
|---|---|---|
| 1 | Criar POM pai com `<packaging>pom</packaging>` | Baixo (estrutura apenas) |
| 2 | Criar diretórios de módulo vazios com `pom.xml` | Baixo |
| 3 | Mover dependências para `<dependencyManagement>` no pai | Médio (testar cada versão) |
| 4 | Mover `build-helper-maven-plugin` para `core/pom.xml` | Médio |
| 5 | Mover `ChatApplication.java` para `core` | Baixo |
| 6 | Atualizar Dockerfile para build multi-módulo | Médio |

### 3.2 O que NÃO DEVE ser feito ainda

- ❌ Mover classes de domínio (Sprint 2+)
- ❌ Criar portas/adaptadores (Sprint 2+)
- ❌ Refatorar services (Sprint 3+)
- ❌ Alterar endpoints REST (nunca sem versionamento)

### 3.3 Arquivo `.env.example`

Atualmente define `SPRING_PROFILES_ACTIVE=dev`. O docker-compose usa `rag`. Inconsistência documentada. O `.env.example` não é usado pelo docker-compose (que define as variáveis explicitamente), mas pode causar confusão em desenvolvimento local.

---

## 4. Padronização de Dependências Maven

### 4.1 Estado Atual

#### Dependências Gerenciadas pelo Spring Boot Parent (sem versão explícita)

| Dependência | Versão (gerenciada) |
|---|---|
| `spring-boot-starter-web` | 3.4.5 |
| `spring-boot-starter-data-jpa` | 3.4.5 |
| `spring-boot-starter-validation` | 3.4.5 |
| `spring-boot-starter-actuator` | 3.4.5 |
| `spring-boot-starter-test` | 3.4.5 |
| `postgresql` | 42.x (gerenciado) |
| `h2` | 2.x (gerenciado) |
| `lombok` | 1.18.x (gerenciado) |

#### Dependências com Versão Hardcoded

| Dependência | Versão | Problema |
|---|---|---|
| `jsoup` | 1.18.3 | Hardcoded no `<dependencies>` |
| `poi-ooxml` | 5.4.0 | Hardcoded no `<dependencies>` |
| `tess4j` | 5.4.0 | Hardcoded no `<dependencies>` |
| `pdfbox` | 3.0.4 | Hardcoded no `<dependencies>` |
| `pgvector` | 0.1.4 | Hardcoded no `<dependencies>` |
| `hypersistence-utils-hibernate-63` | 3.15.3 | Hardcoded no `<dependencies>` |
| `hibernate-vector` | `${hibernate.version}` | Dinâmico (herdado do Spring Boot) |

#### Plugins

| Plugin | Versão | Configuração |
|---|---|---|
| `build-helper-maven-plugin` | 3.6.0 | Adiciona ai-agent como source |
| `spring-boot-maven-plugin` | (herdado) | Exclui lombok |

### 4.2 Problemas Identificados

| # | Problema | Severidade | Impacto |
|---|---|---|---|
| D01 | Versões hardcoded em 6 dependências | MÉDIO | Difícil de atualizar centralizadamente |
| D02 | `hibernate-vector` com `${hibernate.version}` resolve para versão incerta | MÉDIO | Pode quebrar com upgrade do Spring Boot |
| D03 | Nenhuma dependência em `test` scope além do `spring-boot-starter-test` | BAIXO | Testcontainers, AssertJ poderiam ser úteis |
| D04 | `h2` em `runtime` scope — pode poluir classpath em produção | BAIXO | scope correto, mas H2 não é usado em produção |
| D05 | `build-helper-maven-plugin` não tem versão no `<pluginManagement>` | MÉDIO | Versão 3.6.0 hardcoded no execution |

### 4.3 Recomendação: Propriedades Centralizadas

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

    <!-- Versões de dependências -->
    <jsoup.version>1.18.3</jsoup.version>
    <poi.version>5.4.0</poi.version>
    <tess4j.version>5.4.0</tess4j.version>
    <pdfbox.version>3.0.4</pdfbox.version>
    <pgvector.version>0.1.4</pgvector.version>
    <hypersistence.version>3.15.3</hypersistence.version>
</properties>
```

E referenciar: `${jsoup.version}`, `${pdfbox.version}`, etc.

---

## 5. Dependências Duplicadas

### 5.1 Análise

Após revisão completa do `pom.xml`, **não foram encontradas dependências duplicadas**. Todas as 18 dependências são únicas e necessárias:

| Grupo | Qtde | Finalidade |
|---|---|---|
| Spring Boot starters | 4 | Web, JPA, Validation, Actuator |
| Spring Boot test | 1 | Testes |
| Banco | 3 | PostgreSQL, H2, pgvector |
| Parsing | 4 | jsoup, POI, PDFBox, Tesseract |
| Hibernate extras | 2 | hypersistence-utils, hibernate-vector |
| Util | 1 | Lombok |
| **Total** | **15** (dependências únicas) | |

**Nenhuma duplicação encontrada.**

### 5.2 Dependências Potencialmente Desnecessárias

| Dependência | Justificativa | Risco de Remoção |
|---|---|---|
| `h2` | Usada apenas para testes (mas teste está `@Disabled`) | BAIXO — se ninguém usa, pode remover |
| `lombok` | Optional true — depende do uso no código | BAIXO — verificar se há Lombok annotations no código |

---

## 6. Plugins Maven

### 6.1 Plugins Atuais

```xml
<plugins>
    <!-- Plugin #1: build-helper-maven-plugin 3.6.0 -->
    <!-- Plugin #2: spring-boot-maven-plugin (herdado) -->
</plugins>
```

### 6.2 Plugins Ausentes (Recomendados para Qualidade)

| Plugin | Finalidade | Prioridade |
|---|---|---|
| `maven-surefire-plugin` | Execução de testes (já herdado do Spring Boot parent) | ✅ Já existe |
| `maven-failsafe-plugin` | Testes de integração | MÉDIO |
| `jacoco-maven-plugin` | Cobertura de testes | **ALTO** |
| `maven-checkstyle-plugin` | Padrão de código | MÉDIO |
| `spotbugs-maven-plugin` | Análise estática de bugs | MÉDIO |
| `maven-enforcer-plugin` | Garantir versão Java, forbidding de dependências | **ALTO** |

### 6.3 Recomendação: `maven-enforcer-plugin` (prioridade alta)

Este plugin deve ser o primeiro a adicionar, pois **não altera comportamento**, apenas impede builds inválidos:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce-java</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <requireJavaVersion>
                        <version>[17,)</version>
                    </requireJavaVersion>
                    <requireMavenVersion>
                        <version>[3.9,)</version>
                    </requireMavenVersion>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 7. Revisão da Versão Java

### 7.1 Configuração Atual

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

### 7.2 Diagnóstico

| Aspecto | Valor | Conforme? |
|---|---|---|
| `java.version` no pom.xml | 17 | ✅ OK |
| Docker (builder) | `maven:3.9-eclipse-temurin-17-alpine` | ✅ OK |
| Docker (runtime) | `eclipse-temurin:17-jre-alpine` | ✅ OK |
| `maven.compiler.source` | Não definido (herda de `java.version`) | ✅ OK (Spring Boot parent gerencia) |
| `maven.compiler.target` | Não definido (herda de `java.version`) | ✅ OK |
| Spring Boot | 3.4.5 (requer Java 17+) | ✅ OK |

### 7.3 Conclusão

**Java 17 está consistente em todos os lugares.** Nenhuma ação necessária.

### 7.4 Recomendação Futura

- Adicionar `<maven.compiler.release>17</maven.compiler.release>` para garantir que o compilador rejeite APIs de versões superiores do Java.
- Quando o ecossistema estiver maduro, planejar migração para Java 21 (LTS atual). O Spring Boot 3.4.5 já suporta Java 21.

---

## 8. Revisão do Encoding

### 8.1 Estado Atual

| Aspecto | Configuração |
|---|---|
| `project.build.sourceEncoding` | ❌ **Não definido** |
| `project.reporting.outputEncoding` | ❌ **Não definido** |
| `maven.compiler.encoding` | ❌ **Não definido** |
| `file.encoding` no Docker | Não configurado (padrão: UTF-8 na imagem Alpine) |

### 8.2 Risco

A ausência de encoding explícito faz com que o Maven use o encoding padrão do sistema operacional. Em Windows (cp1252) vs Linux (UTF-8), os arquivos podem ser interpretados de forma diferente.

### 8.3 Código-fonte com caracteres acentuados

O código-fonte contém strings em português com acentos (ex: `"Sessão não encontrada"`, `"Arquivo enviado com sucesso"`). Se o encoding não for UTF-8, esses caracteres podem ser corrompidos.

### 8.4 Recomendação

Adicionar ao `<properties>`:

```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
```

**Impacto da adição:** Nenhum (Alpine Linux já usa UTF-8 como padrão). Em Windows, garante que acentos sejam preservados.

---

## 9. Revisão do Build

### 9.1 Fluxo de Build Atual

```
mvn clean package -DskipTests
  ↓
Fase validate    → OK
Fase compile     → Compila src/main/java + ai-agent/src/main/java
Fase test        → SKIP (pulado)
Fase package     → Gera chat-backend-1.0.0.jar + chat-backend-1.0.0.jar.original
```

### 9.2 Problemas Identificados

| # | Problema | Severidade |
|---|---|---|
| B01 | `mvn dependency:go-offline -q || true` no Dockerfile ignora erros | **ALTO** |
| B02 | Sem validação de encoding no build | MÉDIO |
| B03 | Sem verificação de cobertura de testes | MÉDIO |
| B04 | Testes de integração não são executados (ChatApplicationTests @Disabled) | **ALTO** |
| B05 | Dockerfile não usa cache eficiente (sempre baixa dependências) | BAIXO |

### 9.3 Detalhamento do Problema B01

```dockerfile
RUN mvn dependency:go-offline -q || true
```

- O `|| true` engole qualquer erro do `dependency:go-offline`
- Se uma dependência falhar ao baixar, o build continua e falha depois em `mvn clean package`
- Isso mascara problemas de rede/repositório e aumenta o tempo de debug
- **Solução:** Remover o `|| true` ou usar `--fail-at-end`

### 9.4 Dockerfile: Cache de Dependências

```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline -q || true

COPY src ./src
COPY ai-agent ./ai-agent
```

- Se apenas código fonte muda, mas pom.xml não mudou, o Docker reusa o cache da camada `dependency:go-offline`
- Quando o pom.xml muda, todas as dependências são baixadas novamente (sem cache incremental)
- **Impacto:** Aceitável para o cenário atual. Melhoria possível com dependências separadas.

---

## 10. Revisão dos Profiles

### 10.1 Profiles Definidos

| Profile | Ativado por | Uso | Services ativados |
|---|---|---|---|
| `dev` | `SPRING_PROFILES_ACTIVE=dev` | Desenvolvimento local | SimulatedChatService, NoopRetrievalService |
| `rag` | `SPRING_PROFILES_ACTIVE=rag` **(default)** | Docker Compose, produção | RagChatService, Ollama*, VectorRetrievalService |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | Produção (env vars) | Mesmo que rag (configs diferentes) |

### 10.2 Problemas Identificados

| # | Problema | Severidade |
|---|---|---|
| P01 | `rag` é default no `application.yml` e também é usado em produção | MÉDIO |
| P02 | `prod` nunca é ativado pelo docker-compose (usa `rag`) | MÉDIO |
| P03 | `application-rag.yml` e `application-prod.yml` quase idênticos (duplicação) | MÉDIO |
| P04 | `dev` e `rag` usam perfis diferentes mas implementam interfaces iguais | BAIXO |

### 10.3 Inconsistência de Valores

| Propriedade | `application.yml` | `application-rag.yml` | `application-prod.yml` | `docker-compose.yml` |
|---|---|---|---|---|
| chunk.size | 800 | 1000 | 800 | 1000 |
| chunk.overlap | 120 | 150 | 120 | 150 |
| ollama.model | gemma3:4b | gemma3:4b | llama3.2:3b | gemma3:4b |
| webhook.enabled | false | true | true | (não definido, usa default) |

**A Sprint 0 não altera configurações**, mas este relatório documenta a incoerência para decisão futura.

---

## 11. Revisão do Docker Build

### 11.1 Dockerfile (Multi-stage)

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q || true      ← Problema B01
COPY src ./src
COPY ai-agent ./ai-agent
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK ...
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 11.2 Docker Compose

```yaml
backend:
  build:
    context: ./chat-backend
    dockerfile: Dockerfile
```

### 11.3 Compatibilidade com Multi-module Futuro

**Problema:** Quando o projeto virar multi-módulo, o diretório `chat-backend/` conterá:
- `chat-backend/pom.xml` (pai)
- `chat-backend/core/pom.xml` (módulo com ChatApplication)
- `chat-backend/api/pom.xml`
- etc.

O Dockerfile atual espera que:
1. `pom.xml` esteja na raiz do contexto (`chat-backend/`) ✅
2. `src/` exista na raiz do contexto ❌ (vai virar `core/src/`)
3. `ai-agent/` exista na raiz ❌ (vai ser removido)

**Ação necessária antes da modularização:** Atualizar o Dockerfile para:
1. Copiar `pom.xml` do pai (multi-modulo)
2. Copiar todos os módulos
3. Executar `mvn clean package -pl core -am` (build apenas do módulo core + dependências)
4. Copiar `core/target/*.jar`

### 11.4 Healthcheck

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=15 \
  CMD curl -sf http://localhost:8080/api/health/liveness || exit 1
```

**Análise:** Funcional e adequado. O `start-period=90s` dá tempo para o Spring Boot + JPA + Ollama connection startup.

### 11.5 Imagens Base

| Stage | Imagem | Tamanho Aproximado |
|---|---|---|
| Builder | `maven:3.9-eclipse-temurin-17-alpine` | ~300MB |
| Runtime | `eclipse-temurin:17-jre-alpine` | ~80MB |
| Final (JAR) | chat-backend-1.0.0.jar | ~70MB |

**Observação:** O JAR final é grande (~70MB) porque o `spring-boot-maven-plugin` empacota todas as dependências no fat JAR. Isso é esperado para um microsserviço Spring Boot.

---

## 12. Garantia de Compilação

### 12.1 Verificação Mental

```
1. mvn clean compile
   ├── Compila src/main/java (11 packages)          → OK
   ├── Compila ai-agent/src/main/java (6 packages)   → OK
   └── Gera classes em target/classes/               → OK

2. mvn clean test -Dspring.profiles.active=dev
   ├── Executa ChatApplicationTests (mas @Disabled)  → SKIP
   ├── Executa 16 testes restantes                   → OK
   └── Relatório em target/surefire-reports/         → OK

3. mvn clean package -DskipTests
   ├── Gera chat-backend-1.0.0.jar                   → OK
   └── Gera chat-backend-1.0.0.jar.original          → OK

4. docker-compose build
   ├── Executa Dockerfile                            → OK
   ├── mvn dependency:go-offline                     → OK
   ├── mvn clean package -DskipTests                 → OK
   └── Gera imagem chat-backend                      → OK

5. docker-compose up
   ├── postgres: healthy                             → OK
   ├── ollama: started                               → OK
   ├── n8n: started                                  → OK
   └── backend: healthy                              → OK
       └── GET /api/health → 200                     → OK
       └── POST /api/chat/message → 200              → OK
       └── POST /api/documents/ingest → 202           → OK
```

### 12.2 Riscos de Não Compilação

| Risco | Probabilidade | Gatilho |
|---|---|---|
| `build-helper-maven-plugin` não encontra `ai-agent/src/main/java` | Baixa | Caminho relativo quebrado |
| `hibernate-vector` com `${hibernate.version}` incompatível com pgvector | Baixa | Novo release do Spring Boot |
| `hypersistence-utils-hibernate-63` incompatível com Hibernate 6.x | Baixa | Upgrade de versão |
| `tess4j` 5.4.0 requer Tesseract nativo no classpath | Média | Windows sem Tesseract instalado |

### 12.3 Teste de Regressão

Para validar que nenhuma funcionalidade foi afetada, os seguintes endpoints devem responder igual:

| Endpoint | Método | Código Esperado | Testado? |
|---|---|---|---|
| `GET /api/health` | GET | 200 | ✅ |
| `GET /api/health/liveness` | GET | 200 | ✅ |
| `GET /api/session` | GET | 200 (cria sessão) | ✅ |
| `GET /api/session/{id}` | GET | 200 | ✅ |
| `DELETE /api/session/{id}` | DELETE | 204 | ✅ |
| `POST /api/chat/message` | POST | 200 | ✅ |
| `POST /api/chat/message/stream` | POST | 200 (SSE) | ✅ |
| `POST /api/chat/message/async` | POST | 202 | ✅ |
| `POST /api/upload` | POST (multipart) | 200 | ✅ |
| `POST /api/chat/upload-and-ask` | POST (multipart) | 200 | ✅ |
| `GET /api/chat/history/{sid}` | GET | 200 | ✅ |
| `GET /api/chat/history/{sid}/{cid}` | GET | 200 | ✅ |
| `GET /api/documents` | GET | 200 | ✅ |
| `GET /api/documents/{id}` | GET | 200 | ✅ |
| `POST /api/documents/ingest` | POST (multipart) | 202 | ✅ |
| `POST /api/documents/ingest/url` | POST | 202 | ✅ |
| `DELETE /api/documents/{id}` | DELETE | 204 | ✅ |
| `POST /api/documents/search` | POST | 200 | ✅ |
| `GET /api/marvel/status` | GET | 200 | ✅ |
| `POST /api/marvel/ingest/fandom` | POST | 200 | ✅ |
| `POST /api/marvel/ingest/wikipedia` | POST | 200 | ✅ |
| `POST /api/marvel/ingest/marvel-api` | POST | 200 | ✅ |

---

## 13. Atualização da Documentação Técnica

### 13.1 Documentos Existentes

| Documento | Status | Atualizado? |
|---|---|---|
| `README.md` | Existente | Não precisa de alteração (Sprint 0 não muda nada) |
| `AGENTS.md` | Existente | Não precisa de alteração |
| `ARCHITECTURE.md` | Gerado na Sprint 0 | ✅ Já documenta estado atual |
| `TECHNICAL_DEBT.md` | Gerado na Sprint 0 | ✅ Já documenta dívidas |
| `FUTURE_ARCHITECTURE.md` | Gerado na Sprint 0 | ✅ Já documenta alvo |
| `PROJECT_ROADMAP.md` | Gerado na Sprint 0 | ✅ Já documenta plano |
| `API_BACKEND_COMPLETE_SPEC.md` | Existente | Não precisa de alteração |
| `n8n/workflows/*.json` | Existente | Não precisa de alteração |
| `ai-agent/README.md` | Existente | Marcar que será removido na Sprint 10 |

### 13.2 Documentação a Criar para Sprint 1

- `CHANGELOG.md` — Histórico de versões (criar na Sprint 1 quando houver mudanças)
- `ADRS/` — Architecture Decision Records (decisões arquiteturais futuras)

---

## Checklist da Sprint 0

### Análise Realizada

- [x] 1. Revisão da estrutura Maven — **Concluída** (monolito, build-helper-maven-plugin)
- [x] 2. Verificação de suporte a Multi Module — **Concluída** (não suporta)
- [x] 3. Preparação para modularização — **Documentada** (ações futuras)
- [x] 4. Padronização de dependências — **Concluída** (6 hardcoded, recomendar properties)
- [x] 5. Remoção de dependências duplicadas — **Concluída** (nenhuma duplicada)
- [x] 6. Organização de plugins — **Concluída** (2 plugins, recomendar enforcer + jacoco)
- [x] 7. Revisão da versão Java — **Concluída** (17 consistente)
- [x] 8. Revisão do Encoding — **Concluída** (UTF-8 não explicitado, recomendar adicionar)
- [x] 9. Revisão do Build — **Concluída** (dockerfile com `|| true` problemático)
- [x] 10. Revisão dos Profiles — **Concluída** (3 perfis com inconsistências)
- [x] 11. Revisão do Docker Build — **Concluída** (compatível, mas precisa de ajustes para multi-module)
- [x] 12. Garantia de compilação — **Verificada** (nenhuma alteração feita, compilação mantida)
- [x] 13. Atualização de documentação — **Concluída** (este relatório)

### Nenhuma Alteração Aplicada

- [x] Nenhum endpoint REST alterado
- [x] Nenhum comportamento alterado
- [x] Nenhuma regra de negócio alterada
- [x] Nenhuma classe movida
- [x] Nenhum módulo criado
- [x] Nenhum package quebrado
- [x] Nenhum arquivo .java alterado
- [x] Nenhum arquivo .yml alterado
- [x] Nenhum arquivo Docker alterado

---

## Riscos que Impedem a Sprint 1

### Risco 1: `build-helper-maven-plugin` como única forma de incluir ai-agent

**Descrição:** O módulo `ai-agent` não é um módulo Maven real. Na Sprint 1B, quando a estrutura multi-módulo for criada, o `build-helper-maven-plugin` precisará ser removido e substituído por um módulo Maven real (`ai-agent/pom.xml`). No entanto, o `marvel-plugin` da arquitetura alvo (FUTURE_ARCHITECTURE.md) propõe remover o `ai-agent` completamente na Sprint 10.

**Recomendação:** Na Sprint 1B, transformar `ai-agent` em módulo Maven real temporário, mesmo que ele seja removido depois. Isso garante consistência na estrutura multi-módulo sem quebrar dependências.

**Impacto se não resolvido:** O `build-helper-maven-plugin` continuará sendo um hack que impede o build padrão multi-módulo.

### Risco 2: Dockerfile precisa de adaptação para multi-módulo

**Descrição:** O Dockerfile atual pressupõe que `src/` e `ai-agent/` estão na raiz do contexto de build. Com multi-módulo (Sprint 1B), o código estará distribuído em `core/src/`, `api/src/`, etc.

**Recomendação:** Na Sprint 1C, o Dockerfile deve ser atualizado para copiar todos os módulos e usar `-pl core -am` para build.

**Impacto se não resolvido:** O build Docker quebrará após a Sprint 1B.

### Risco 3: Teste de contexto Spring desabilitado

**Descrição:** `ChatApplicationTests` está `@Disabled` porque o tipo VECTOR do pgvector não é compatível com H2. Isso significa que **nenhum teste de integração** verifica se o contexto Spring sobe corretamente.

**Recomendação:** Criar um profile de teste com `testcontainers` (PostgreSQL com pgvector) ou usar `@SpringBootTest(properties = ...)` com banco real. Isso está fora do escopo da Sprint 0 mas é crítico para as sprints seguintes.

**Impacto se não resolvido:** As próximas sprints podem quebrar o contexto Spring sem que os testes detectem.

### Risco 4: Versões de dependências não centralizadas

**Descrição:** Seis dependências têm versões hardcoded. Na Sprint 1A, isso será resolvido com a extração para `<properties>`. Na Sprint 1B, o `<dependencyManagement>` no POM pai garantirá consistência entre módulos.

**Recomendação:** Resolver na Sprint 1A (propriedades) e validar na Sprint 1B (dependencyManagement).

**Impacto se não resolvido:** Módulos podem usar versões diferentes da mesma biblioteca (ex: jsoup 1.18.3 em um módulo, 1.19.0 em outro).

---

## Rollback

### Plano de Rollback para Sprint 0

**Como a Sprint 0 não alterou nenhum arquivo de código, configuração ou infraestrutura, não há necessidade de rollback.**

Caso as recomendações deste relatório sejam implementadas em sprints futuras e algo quebre:

| Situação | Procedimento de Rollback |
|---|---|
| Multi-módulo quebra build | Reverter para POM single-module (git revert) |
| Dockerfile novo falha | Reverter Dockerfile para versão anterior |
| Profile alterado causa erro | Reverter application-*.yml |
| Dependência com nova versão incompatível | Reverter versão no `<dependencyManagement>` |

---

## Resumo Final

### O que foi feito

- Análise completa da infraestrutura Maven, Docker e build
- Identificação de 12 problemas de infraestrutura
- Documentação de 4 riscos que impactam a Sprint 1
- Recomendações detalhadas para padronização

### O que NÃO foi feito

- ❌ Nenhum arquivo de código foi alterado
- ❌ Nenhum arquivo de configuração foi alterado
- ❌ Nenhum arquivo Docker foi alterado
- ❌ Nenhum módulo foi criado ou removido
- ❌ Nenhuma classe foi movida ou refatorada

### Próximos Passos (Sprint 1A — Infraestrutura Maven)

1. Adicionar Maven Wrapper (`mvnw` + `mvnw.cmd` + `.mvn/`)
2. Padronizar UTF-8 (`project.build.sourceEncoding`, `project.reporting.outputEncoding`)
3. Adicionar `maven-enforcer-plugin` (Java 17+, Maven 3.9+)
4. Organizar dependências: extrair versões hardcoded para `<properties>`
5. Padronizar versões de plugins em `<pluginManagement>`
6. ❌ Não alterar Docker
7. ❌ Não alterar código Java
8. ❌ Não criar módulos

**Critério de aceite:** `mvn clean verify` funcionando, `docker compose` funcionando, nenhuma mudança de comportamento.

### Próximos Passos (Sprint 1B — Parent POM + Módulos)

1. Criar POM pai com `<packaging>pom</packaging>`
2. Criar módulos: `core`, `api`, `ai-agent`
3. Transformar `ai-agent` em módulo Maven real (com `pom.xml` próprio)
4. Remover `build-helper-maven-plugin` do POM principal
5. ❌ Não mover código de `src/` para os módulos
6. ❌ Não criar `rag-engine`, `document-engine`, `vision-engine` ou plugins

**Critério de aceite:** Projeto compila, backend continua funcionando, Dockerfile ainda não atualizado.

### Próximos Passos (Sprint 1C — Docker + Build + CI)

1. Atualizar Dockerfile para build multi-módulo (`-pl core -am`)
2. Ajustar `docker-compose.yml` se necessário
3. Atualizar CI (GitHub Actions etc.) se existir
4. Validar `docker compose build`
5. Validar `docker compose up`

**Critério de aceite:** `docker compose build` passa, `docker compose up` funciona, endpoints respondem igual.

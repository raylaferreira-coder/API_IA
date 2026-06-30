# Changelog

## [2.0.0] - 2026-06-30

### Added
- Sistema RAG (Retrieval-Augmented Generation) com pgvector
- Chat com contexto baseado em documentos
- Upload e processamento de múltiplos formatos (PDF, TXT, DOCX, HTML, MD, imagens com OCR)
- Busca vetorial semântica com embeddings (nomic-embed-text)
- Agente IA especialista no Universo Marvel (ai-agent)
- Integração com Ollama para LLM local (Gemma 3)
- Pipeline de ingestão automática com n8n (Fandom + Wikipedia)
- Integração com Marvel API oficial
- Docker Compose completo (PostgreSQL+pgvector, Ollama, n8n, Backend, Frontend)
- Streaming SSE para respostas do chat
- Health checks (liveness, readiness)
- Sessões de conversa com expiração automática
- Perfis Spring (dev, rag, prod)
- Migração Flyway para extensão vector do PostgreSQL

### Changed
- N/A — versão inicial do sistema RAG

### Fixed
- N/A — versão inicial do sistema RAG

## [1.0.0] - 2025-12-15

### Added
- Versão inicial do backend com chat simples
- Endpoints básicos de health check
- Configuração Docker inicial

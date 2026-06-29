package com.project.chat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)");
            log.info("Índice pgvector IVFFlat criado/verificado em document_chunks.embedding");
        } catch (Exception e) {
            log.warn("Não foi possível criar índice pgvector (pode já existir ou extensão não habilitada): {}", e.getMessage());
        }
    }
}
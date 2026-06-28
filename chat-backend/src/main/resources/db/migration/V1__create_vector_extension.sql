CREATE EXTENSION IF NOT EXISTS vector;

CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
    ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

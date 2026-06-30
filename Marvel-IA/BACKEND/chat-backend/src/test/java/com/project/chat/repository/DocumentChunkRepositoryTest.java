package com.project.chat.repository;

import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Disabled("Requer PostgreSQL + pgvector (VECTOR type incompatível com H2)")
class DocumentChunkRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void findByDocumentIdOrderByChunkIndex_ShouldReturnChunks() {
        Document doc = new Document("teste.pdf", "/path/teste.pdf", "PDF");
        entityManager.persist(doc);

        DocumentChunk chunk1 = new DocumentChunk(doc, 0, "Conteudo 1", 3);
        DocumentChunk chunk2 = new DocumentChunk(doc, 1, "Conteudo 2", 3);
        entityManager.persist(chunk1);
        entityManager.persist(chunk2);
        entityManager.flush();

        List<DocumentChunk> found = documentChunkRepository
                .findByDocumentId(doc.getId());

        assertEquals(2, found.size());
    }

    @Test
    void deleteByDocumentId_ShouldRemoveChunks() {
        Document doc = new Document("teste.pdf", "/path/teste.pdf", "PDF");
        entityManager.persist(doc);

        DocumentChunk chunk = new DocumentChunk(doc, 0, "Conteudo", 3);
        entityManager.persist(chunk);
        entityManager.flush();

        documentChunkRepository.deleteByDocumentId(doc.getId());

        List<DocumentChunk> found = documentChunkRepository
                .findByDocumentId(doc.getId());
        assertTrue(found.isEmpty());
    }
}

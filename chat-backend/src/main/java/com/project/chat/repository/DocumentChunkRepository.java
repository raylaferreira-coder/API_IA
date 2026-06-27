package com.project.chat.repository;

import com.project.chat.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query(value = """
        SELECT * FROM document_chunks
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
    """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(@Param("embedding") String embeddingStr, @Param("limit") int limit);

    @Query(value = """
        SELECT * FROM document_chunks
        WHERE document_id = :documentId
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
    """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksByDocument(
            @Param("documentId") Long documentId,
            @Param("embedding") String embeddingStr,
            @Param("limit") int limit);
}

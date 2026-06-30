package com.project.chat.repository;

import com.project.chat.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    @Query("SELECT m FROM DocumentMetadata m WHERE m.document.id = :documentId")
    List<DocumentMetadata> findByDocumentId(@Param("documentId") Long documentId);

    @Modifying
    @Query("DELETE FROM DocumentMetadata m WHERE m.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}

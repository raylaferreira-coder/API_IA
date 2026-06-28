package com.project.chat.repository;

import com.project.chat.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {

    List<DocumentMetadata> findByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}

package com.project.chat.repository;

import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByStatus(DocumentStatus status);
    List<Document> findBySourceTypeOrderByCreatedAtDesc(String sourceType);
}

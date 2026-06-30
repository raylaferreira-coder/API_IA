package com.project.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentStatus;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByStatus(DocumentStatus status);

    List<Document> findBySourceTypeOrderByCreatedAtDesc(String sourceType);

    boolean existsBySourcePath(String sourcePath);

}

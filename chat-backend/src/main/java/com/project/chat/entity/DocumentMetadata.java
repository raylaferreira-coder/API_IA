package com.project.chat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "document_metadata")
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    public DocumentMetadata() {}

    public DocumentMetadata(Long documentId, String key, String value) {
        this.documentId = documentId;
        this.key = key;
        this.value = value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

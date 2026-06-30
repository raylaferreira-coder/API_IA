package com.project.chat.dto.response;

import java.util.List;

public class DocumentsListResponse {

    private List<DocumentResponse> documents;

    public DocumentsListResponse() {}

    public DocumentsListResponse(List<DocumentResponse> documents) {
        this.documents = documents;
    }

    public List<DocumentResponse> getDocuments() { return documents; }
    public void setDocuments(List<DocumentResponse> documents) { this.documents = documents; }
}

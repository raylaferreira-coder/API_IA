package com.project.chat.controller;

import com.project.chat.dto.request.IngestUrlRequest;
import com.project.chat.dto.request.SearchRequest;
import com.project.chat.dto.response.DocumentChunkResponse;
import com.project.chat.dto.response.DocumentResponse;
import com.project.chat.dto.response.IngestionResponse;
import com.project.chat.dto.response.SearchResultResponse;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.mapper.DocumentMapper;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.service.DocumentIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public DocumentController(DocumentIngestionService ingestionService,
                              DocumentRepository documentRepository,
                              DocumentMapper documentMapper) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    @PostMapping("/ingest/url")
    public ResponseEntity<IngestionResponse> ingestUrl(@Valid @RequestBody IngestUrlRequest request) {
        Document document = ingestionService.ingestFromUrl(request.getUrl());
        String message = "URL ingerida com sucesso.";
        if (document.getStatus() == DocumentStatus.FAILED) {
            message = "Falha ao ingerir URL: " + document.getErrorMessage();
        }
        return ResponseEntity.ok(documentMapper.toIngestionResponse(document, message));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments() {
        List<Document> documents = documentRepository.findAll();
        List<DocumentResponse> response = documents.stream()
                .map(documentMapper::toDocumentResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(doc -> ResponseEntity.ok(documentMapper.toDocumentResponse(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResultResponse> search(@Valid @RequestBody SearchRequest request) {
        List<DocumentChunk> chunks = ingestionService.searchSimilar(request.getQuery(), request.getLimit());
        List<DocumentChunkResponse> results = chunks.stream()
                .map(documentMapper::toChunkResponse)
                .toList();
        return ResponseEntity.ok(new SearchResultResponse(request.getQuery(), results.size(), results));
    }
}

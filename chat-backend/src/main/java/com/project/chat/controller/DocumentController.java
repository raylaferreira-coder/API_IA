package com.project.chat.controller;

import com.project.chat.dto.request.IngestUrlRequest;
import com.project.chat.dto.request.SearchRequest;
import com.project.chat.dto.response.DocumentChunkResponse;
import com.project.chat.dto.response.DocumentResponse;
import com.project.chat.dto.response.DocumentsListResponse;
import com.project.chat.dto.response.IngestionResponse;
import com.project.chat.dto.response.SearchResultResponse;
import com.project.chat.entity.Document;
import com.project.chat.exception.FileTooLargeException;
import com.project.chat.exception.IngestionException;
import com.project.chat.exception.UnsupportedFileTypeException;
import com.project.chat.mapper.DocumentMapper;
import com.project.chat.service.DocumentIngestionService;
import com.project.chat.service.DocumentQueryService;
import com.project.chat.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentQueryService documentQueryService;
    private final DocumentMapper documentMapper;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentIngestionService ingestionService,
                              DocumentQueryService documentQueryService,
                              DocumentMapper documentMapper,
                              FileStorageService fileStorageService) {
        this.ingestionService = ingestionService;
        this.documentQueryService = documentQueryService;
        this.documentMapper = documentMapper;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/ingest/url")
    public ResponseEntity<IngestionResponse> ingestUrl(@Valid @RequestBody IngestUrlRequest request) {
        Document document = ingestionService.ingestFromUrl(request.getUrl());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(documentMapper.toIngestionResponse(document, "URL enviada para processamento."));
    }

    @GetMapping
    public ResponseEntity<DocumentsListResponse> listDocuments() {
        return ResponseEntity.ok(documentQueryService.listDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentQueryService.getDocument(id));
    }

    private static final long MAX_INGESTION_FILE_SIZE = 50L * 1024 * 1024;

    private static final Set<String> SUPPORTED_INGESTION_TYPES = Set.of(
            "txt", "pdf", "markdown", "md", "html", "htm");

    @PostMapping("/ingest")
    public ResponseEntity<IngestionResponse> ingestFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", required = false) String sourceType) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }

        if (file.getSize() > MAX_INGESTION_FILE_SIZE) {
            throw new FileTooLargeException("O arquivo excede o limite máximo de 50 MB para indexação.");
        }

        String detectedType = sourceType;
        if (detectedType == null || detectedType.isBlank()) {
            detectedType = detectSourceType(file.getOriginalFilename());
        }

        if (!SUPPORTED_INGESTION_TYPES.contains(detectedType.toLowerCase())) {
            throw new UnsupportedFileTypeException(
                    "Formato de arquivo não suportado para indexação. Utilize .pdf, .txt, .md ou .html.");
        }

        try {
            Path storagePath = fileStorageService.store(file);
            Document document = ingestionService.ingestFromFile(
                    storagePath.toString(), detectedType, file.getOriginalFilename());
            document.setFileSize(file.getSize());
            ingestionService.updateDocument(document);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(documentMapper.toIngestionResponse(document, "Documento enviado para processamento."));
        } catch (IOException e) {
            throw new IngestionException("Erro ao processar o arquivo: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentQueryService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{documentId}/chunks")
    public ResponseEntity<List<DocumentChunkResponse>> getDocumentChunks(@PathVariable Long documentId) {
        return ResponseEntity.ok(documentQueryService.getDocumentChunks(documentId));
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResultResponse> search(@Valid @RequestBody SearchRequest request) {
        return ResponseEntity.ok(documentQueryService.searchSimilar(request.getQuery(), request.getTopK()));
    }

    private String detectSourceType(String fileName) {
        if (fileName == null) return "txt";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "markdown";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        return "txt";
    }
}

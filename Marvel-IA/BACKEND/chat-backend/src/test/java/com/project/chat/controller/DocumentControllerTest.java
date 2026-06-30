package com.project.chat.controller;

import com.project.chat.dto.response.DocumentChunkResponse;
import com.project.chat.dto.response.DocumentResponse;
import com.project.chat.dto.response.DocumentsListResponse;
import com.project.chat.dto.response.IngestionResponse;
import com.project.chat.dto.response.SearchResultResponse;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.mapper.DocumentMapper;
import com.project.chat.service.DocumentIngestionService;
import com.project.chat.service.DocumentQueryService;
import com.project.chat.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentIngestionService ingestionService;
    @Mock
    private DocumentQueryService documentQueryService;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private FileStorageService fileStorageService;

    private DocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new DocumentController(
                ingestionService, documentQueryService,
                documentMapper, fileStorageService);
    }

    @Test
    void listDocuments_ShouldReturnAllDocuments() {
        DocumentResponse docResponse = new DocumentResponse(
                1L, "teste.txt", "TXT",
                1000L, DocumentStatus.COMPLETED, 5,
                LocalDateTime.now());
        when(documentQueryService.listDocuments())
                .thenReturn(new DocumentsListResponse(List.of(docResponse)));

        ResponseEntity<DocumentsListResponse> response = controller.listDocuments();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getDocuments().size());
        assertEquals("teste.txt", response.getBody().getDocuments().get(0).getFileName());
    }

    @Test
    void listDocuments_WhenEmpty_ShouldReturnEmptyList() {
        when(documentQueryService.listDocuments())
                .thenReturn(new DocumentsListResponse(List.of()));

        ResponseEntity<DocumentsListResponse> response = controller.listDocuments();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getDocuments().isEmpty());
    }

    @Test
    void getDocument_WhenFound_ShouldReturnDocument() {
        DocumentResponse docResponse = new DocumentResponse(
                1L, "teste.txt", "TXT",
                null, DocumentStatus.PENDING, 0,
                LocalDateTime.now());
        when(documentQueryService.getDocument(1L)).thenReturn(docResponse);

        ResponseEntity<DocumentResponse> response = controller.getDocument(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("teste.txt", response.getBody().getFileName());
    }

    @Test
    void getDocument_WhenNotFound_ShouldThrowException() {
        when(documentQueryService.getDocument(999L))
                .thenThrow(new ResourceNotFoundException("Documento não encontrado: 999"));

        assertThrows(ResourceNotFoundException.class, () -> controller.getDocument(999L));
    }

    @Test
    void deleteDocument_WhenFound_ShouldReturn204() {
        doNothing().when(documentQueryService).deleteDocument(1L);

        ResponseEntity<Void> response = controller.deleteDocument(1L);

        assertEquals(204, response.getStatusCodeValue());
        verify(documentQueryService).deleteDocument(1L);
    }

    @Test
    void deleteDocument_WhenNotFound_ShouldThrowException() {
        doThrow(new ResourceNotFoundException("Documento não encontrado: 999"))
                .when(documentQueryService).deleteDocument(999L);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteDocument(999L));
    }
}

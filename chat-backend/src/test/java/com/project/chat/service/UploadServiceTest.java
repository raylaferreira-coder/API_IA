package com.project.chat.service;

import com.project.chat.dto.response.UploadResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.entity.Session;
import com.project.chat.exception.FileTooLargeException;
import com.project.chat.exception.UnsupportedFileTypeException;
import com.project.chat.mapper.AttachmentMapper;
import com.project.chat.repository.AttachmentRepository;
import com.project.chat.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AttachmentMapper attachmentMapper;
    @Mock
    private SessionRepository sessionRepository;

    private UploadService uploadService;
    private static final String VALID_SESSION = "valid-session-id";

    @BeforeEach
    void setUp() {
        uploadService = new UploadService(attachmentRepository, fileStorageService, attachmentMapper, sessionRepository);
    }

    private void mockValidSession() {
        Session session = new Session(VALID_SESSION);
        when(sessionRepository.findBySessionId(VALID_SESSION)).thenReturn(Optional.of(session));
    }

    @Test
    void uploadFile_WithValidTxtFile_ShouldReturnUploadResponse() throws IOException {
        mockValidSession();
        MultipartFile file = new MockMultipartFile(
                "file", "teste.txt", "text/plain", "conteudo".getBytes());
        Attachment attachment = new Attachment("teste.txt", "text/plain", 8L, "/path/file.txt");
        attachment.setId(1L);
        attachment.setUploadedAt(LocalDateTime.now());
        UploadResponse expectedResponse = new UploadResponse(
                1L, "teste.txt", "text/plain", 8L, attachment.getUploadedAt(), "Arquivo enviado com sucesso.");

        when(fileStorageService.store(any(MultipartFile.class))).thenReturn(Path.of("/path/file.txt"));
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);
        when(attachmentMapper.toUploadResponse(attachment)).thenReturn(expectedResponse);

        UploadResponse response = uploadService.uploadFile(file, VALID_SESSION);

        assertNotNull(response);
        assertEquals(1L, response.getAttachmentId());
        assertEquals("teste.txt", response.getFileName());
        verify(fileStorageService).store(any(MultipartFile.class));
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void uploadFile_WithNullFile_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> uploadService.uploadFile(null, VALID_SESSION));
    }

    @Test
    void uploadFile_WithEmptyFile_ShouldThrowIllegalArgumentException() {
        MultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> uploadService.uploadFile(file, VALID_SESSION));
    }

    @Test
    void uploadFile_WithOversizedFile_ShouldThrowFileTooLargeException() {
        mockValidSession();
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
                "file", "large.txt", "text/plain", largeContent);

        assertThrows(FileTooLargeException.class,
                () -> uploadService.uploadFile(file, VALID_SESSION));
    }

    @Test
    void uploadFile_WithUnsupportedType_ShouldThrowUnsupportedFileTypeException() {
        mockValidSession();
        MultipartFile file = new MockMultipartFile(
                "file", "image.webp", "image/webp", "fake".getBytes());

        assertThrows(UnsupportedFileTypeException.class,
                () -> uploadService.uploadFile(file, VALID_SESSION));
    }
}

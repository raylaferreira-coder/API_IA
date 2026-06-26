package com.project.chat.service;

import com.project.chat.dto.response.UploadResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.exception.FileTooLargeException;
import com.project.chat.exception.UnsupportedFileTypeException;
import com.project.chat.mapper.AttachmentMapper;
import com.project.chat.repository.AttachmentRepository;
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

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadService(attachmentRepository, fileStorageService, attachmentMapper);
    }

    @Test
    void uploadFile_WithValidTxtFile_ShouldReturnUploadResponse() throws IOException {
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

        UploadResponse response = uploadService.uploadFile(file, "session-1");

        assertNotNull(response);
        assertEquals(1L, response.getAttachmentId());
        assertEquals("teste.txt", response.getFileName());
        verify(fileStorageService).store(any(MultipartFile.class));
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void uploadFile_WithNullFile_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> uploadService.uploadFile(null, "session-1"));
    }

    @Test
    void uploadFile_WithEmptyFile_ShouldThrowIllegalArgumentException() {
        MultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> uploadService.uploadFile(file, "session-1"));
    }

    @Test
    void uploadFile_WithOversizedFile_ShouldThrowFileTooLargeException() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
                "file", "large.txt", "text/plain", largeContent);

        assertThrows(FileTooLargeException.class,
                () -> uploadService.uploadFile(file, "session-1"));
    }

    @Test
    void uploadFile_WithUnsupportedType_ShouldThrowUnsupportedFileTypeException() {
        MultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "fake".getBytes());

        assertThrows(UnsupportedFileTypeException.class,
                () -> uploadService.uploadFile(file, "session-1"));
    }
}

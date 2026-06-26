package com.project.chat.service;

import com.project.chat.dto.response.UploadResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.exception.FileTooLargeException;
import com.project.chat.exception.UnsupportedFileTypeException;
import com.project.chat.repository.AttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@Service
public class UploadService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("text/plain", "application/pdf");

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    public UploadService(AttachmentRepository attachmentRepository,
                         FileStorageService fileStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    public UploadResponse uploadFile(MultipartFile file, String sessionId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException("O arquivo excede o limite máximo de 10 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new UnsupportedFileTypeException(
                    "Formato de arquivo não suportado. Utilize .txt ou .pdf.");
        }

        try {
            Path storagePath = fileStorageService.store(file);
            Attachment attachment = new Attachment(
                    null,
                    file.getOriginalFilename(),
                    contentType,
                    file.getSize(),
                    storagePath.toString()
            );
            attachment = attachmentRepository.save(attachment);

            return new UploadResponse(
                    attachment.getId(),
                    attachment.getFileName(),
                    attachment.getFileType(),
                    attachment.getFileSize(),
                    attachment.getUploadedAt(),
                    "Arquivo enviado com sucesso."
            );
        } catch (IOException e) {
            throw new RuntimeException("Erro ao armazenar o arquivo.", e);
        }
    }
}

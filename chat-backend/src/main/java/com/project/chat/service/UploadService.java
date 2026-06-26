package com.project.chat.service;

import com.project.chat.dto.response.UploadResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.exception.FileTooLargeException;
import com.project.chat.exception.UnsupportedFileTypeException;
import com.project.chat.mapper.AttachmentMapper;
import com.project.chat.repository.AttachmentRepository;
import com.project.chat.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;

    public UploadService(AttachmentRepository attachmentRepository,
                         FileStorageService fileStorageService,
                         AttachmentMapper attachmentMapper) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.attachmentMapper = attachmentMapper;
    }

    public UploadResponse uploadFile(MultipartFile file, String sessionId) {
        if (file == null || file.isEmpty()) {
            log.warn("Tentativa de upload sem arquivo.");
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }

        if (!FileUtils.isWithinSizeLimit(file.getSize())) {
            log.warn("Arquivo excede limite de tamanho: {} bytes", file.getSize());
            throw new FileTooLargeException("O arquivo excede o limite máximo de 10 MB.");
        }

        String contentType = file.getContentType();
        if (!FileUtils.isAllowedMimeType(contentType)) {
            log.warn("Tipo de arquivo não suportado: {}", contentType);
            throw new UnsupportedFileTypeException(
                    "Formato de arquivo não suportado. Utilize .txt ou .pdf.");
        }

        try {
            Path storagePath = fileStorageService.store(file);
            Attachment attachment = new Attachment(
                    file.getOriginalFilename(),
                    contentType,
                    file.getSize(),
                    storagePath.toString()
            );
            attachment = attachmentRepository.save(attachment);
            log.info("Arquivo salvo: id={}, nome={}", attachment.getId(), attachment.getFileName());

            return attachmentMapper.toUploadResponse(attachment);
        } catch (IOException e) {
            log.error("Erro ao armazenar arquivo: ", e);
            throw new RuntimeException("Erro ao armazenar o arquivo.", e);
        }
    }
}

package com.project.chat.mapper;

import com.project.chat.dto.response.UploadResponse;
import com.project.chat.entity.Attachment;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {

    public UploadResponse toUploadResponse(Attachment attachment) {
        return new UploadResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getUploadedAt(),
                "Arquivo enviado com sucesso."
        );
    }
}

package com.project.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public class UploadRequest {

    private MultipartFile file;

    @NotBlank(message = "O identificador de sessão é obrigatório.")
    private String sessionId;

    public UploadRequest() {
    }

    public UploadRequest(MultipartFile file, String sessionId) {
        this.file = file;
        this.sessionId = sessionId;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

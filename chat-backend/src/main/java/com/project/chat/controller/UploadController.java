package com.project.chat.controller;

import com.project.chat.dto.response.ErrorResponse;
import com.project.chat.dto.response.UploadResponse;
import com.project.chat.service.UploadService;
import com.project.chat.util.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            ErrorResponse error = new ErrorResponse(
                    400, "Bad Request",
                    "Nenhum arquivo foi enviado.",
                    request.getRequestURI()
            );
            return ResponseEntity.badRequest().body(error);
        }

        if (!FileUtils.isValidUuid(sessionId)) {
            ErrorResponse error = new ErrorResponse(
                    400, "Bad Request",
                    "O identificador de sessão fornecido é inválido.",
                    request.getRequestURI()
            );
            return ResponseEntity.badRequest().body(error);
        }

        UploadResponse response = uploadService.uploadFile(file, sessionId);
        return ResponseEntity.ok(response);
    }
}

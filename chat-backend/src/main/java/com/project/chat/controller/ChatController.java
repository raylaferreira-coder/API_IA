package com.project.chat.controller;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.request.UploadAndAskRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.service.ChatService;
import com.project.chat.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final FileStorageService fileStorageService;

    public ChatController(ChatService chatService, FileStorageService fileStorageService) {
        this.chatService = chatService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload-and-ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponse> uploadAndAsk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestParam(value = "content", required = false) String content) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }

        Path storagePath;
        try {
            storagePath = fileStorageService.store(file);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao armazenar o arquivo.", e);
        }

        UploadAndAskRequest request = new UploadAndAskRequest(
                sessionId,
                conversationId,
                content,
                storagePath.toString(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        ChatResponse response = chatService.uploadAndAsk(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<HistoryResponse> getHistory(@PathVariable String sessionId) {
        HistoryResponse response = chatService.getHistory(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String sessionId,
            @PathVariable Long conversationId) {
        ConversationResponse response = chatService.getConversation(sessionId, conversationId);
        return ResponseEntity.ok(response);
    }
}

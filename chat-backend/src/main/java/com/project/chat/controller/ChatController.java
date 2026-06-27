package com.project.chat.controller;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.ErrorResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.service.ChatService;
import com.project.chat.util.FileUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId, HttpServletRequest request) {
        if (!FileUtils.isValidUuid(sessionId)) {
            ErrorResponse error = new ErrorResponse(
                    400, "Bad Request",
                    "O identificador de sessão fornecido é inválido.",
                    request.getRequestURI()
            );
            return ResponseEntity.badRequest().body(error);
        }
        HistoryResponse response = chatService.getHistory(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{sessionId}/{conversationId}")
    public ResponseEntity<?> getConversation(
            @PathVariable String sessionId,
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        if (!FileUtils.isValidUuid(sessionId)) {
            ErrorResponse error = new ErrorResponse(
                    400, "Bad Request",
                    "O identificador de sessão fornecido é inválido.",
                    request.getRequestURI()
            );
            return ResponseEntity.badRequest().body(error);
        }
        ConversationResponse response = chatService.getConversation(sessionId, conversationId);
        return ResponseEntity.ok(response);
    }
}

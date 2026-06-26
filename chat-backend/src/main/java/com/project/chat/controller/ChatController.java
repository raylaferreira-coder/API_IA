package com.project.chat.controller;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.service.ChatService;
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

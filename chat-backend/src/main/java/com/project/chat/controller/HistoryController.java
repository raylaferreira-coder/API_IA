package com.project.chat.controller;

import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/history")
public class HistoryController {

    private final ConversationService conversationService;

    public HistoryController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<HistoryResponse> getHistory(@PathVariable String sessionId) {
        HistoryResponse response = conversationService.getHistory(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String sessionId,
            @PathVariable Long conversationId) {
        ConversationResponse response = conversationService.getConversation(sessionId, conversationId);
        return ResponseEntity.ok(response);
    }
}

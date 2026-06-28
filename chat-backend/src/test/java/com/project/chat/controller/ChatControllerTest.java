package com.project.chat.controller;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @Test
    void sendMessage_ShouldReturn200() throws Exception {
        ChatRequest request = new ChatRequest("a1b2c3d4-e5f6-7890-abcd-ef1234567890", null, "Olá", null);
        ChatResponse response = new ChatResponse(null, null, 1L);

        when(chatService.sendMessage(any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getHistory_ShouldReturn200() throws Exception {
        HistoryResponse response = new HistoryResponse("session-1", java.util.Collections.emptyList());

        when(chatService.getHistory("session-1")).thenReturn(response);

        mockMvc.perform(get("/api/chat/history/{sessionId}", "session-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getConversation_ShouldReturn200() throws Exception {
        ConversationResponse response = new ConversationResponse(1L, java.util.Collections.emptyList());

        when(chatService.getConversation("session-1", 1L)).thenReturn(response);

        mockMvc.perform(get("/api/chat/history/{sessionId}/{conversationId}", "session-1", 1L))
                .andExpect(status().isOk());
    }
}

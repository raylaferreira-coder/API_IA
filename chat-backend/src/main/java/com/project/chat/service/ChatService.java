package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.request.UploadAndAskRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    ChatResponse sendMessage(ChatRequest request);

    ChatResponse uploadAndAsk(UploadAndAskRequest request);

    HistoryResponse getHistory(String sessionId);

    ConversationResponse getConversation(String sessionId, Long conversationId);

    String sendMessageAsync(ChatRequest request);

    TaskService.TaskEntry getTaskStatus(String taskId);

    SseEmitter sendMessageStream(ChatRequest request);
}

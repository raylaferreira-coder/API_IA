package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.request.UploadAndAskRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;

public interface ChatService {

    ChatResponse sendMessage(ChatRequest request);

    ChatResponse uploadAndAsk(UploadAndAskRequest request);

    HistoryResponse getHistory(String sessionId);

    ConversationResponse getConversation(String sessionId, Long conversationId);
}

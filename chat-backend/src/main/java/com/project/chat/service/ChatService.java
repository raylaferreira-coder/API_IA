package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;

public interface ChatService {

    ChatResponse sendMessage(ChatRequest request);
}

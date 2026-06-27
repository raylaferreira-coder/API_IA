package com.project.chat.service;

import com.project.chat.ai.MarvelAiAgent;
import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OllamaChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);

    private final MarvelAiAgent marvelAiAgent;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;

    public OllamaChatService(MarvelAiAgent marvelAiAgent,
                             RetrievalService retrievalService,
                             PromptBuilder promptBuilder) {
        this.marvelAiAgent = marvelAiAgent;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public ChatResponse sendMessage(ChatRequest request) {
        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Tentativa de envio de mensagem vazia.");
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }

        log.info("OllamaChatService processando mensagem: sessionId={}, conversationId={}",
                request.getSessionId(), request.getConversationId());

        List<DocumentChunk> chunks = retrievalService.search(content, 5);
        log.info("Recuperados {} chunks relevantes para a consulta", chunks.size());

        String chunksContext = promptBuilder.buildChunkContext(chunks);
        String answer = marvelAiAgent.askWithContext(content, chunksContext);

        log.info("Resposta gerada pelo Ollama ({} caracteres)", answer.length());

        LocalDateTime now = LocalDateTime.now();

        MessageResponse userMsg = new MessageResponse(null, null, "USER", content, now);
        MessageResponse assistantMsg = new MessageResponse(null, null, "ASSISTANT", answer, now);

        return new ChatResponse(userMsg, assistantMsg, null);
    }

    @Override
    public HistoryResponse getHistory(String sessionId) {
        throw new UnsupportedOperationException(
                "Histórico de conversas será implementado quando o banco de dados estiver disponível.");
    }

    @Override
    public ConversationResponse getConversation(String sessionId, Long conversationId) {
        throw new UnsupportedOperationException(
                "Recuperação de conversa será implementada quando o banco de dados estiver disponível.");
    }

}

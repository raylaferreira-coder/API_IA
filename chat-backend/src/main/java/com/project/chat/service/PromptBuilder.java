package com.project.chat.service;

import com.project.chat.ai.prompt.MarvelPromptBuilder;
import com.project.chat.entity.Message;
import com.project.chat.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilder.class);
    private static final int MAX_HISTORY_MESSAGES = 10;

    private final MarvelPromptBuilder marvelPromptBuilder;
    private final MessageRepository messageRepository;

    public PromptBuilder(MarvelPromptBuilder marvelPromptBuilder,
                         MessageRepository messageRepository) {
        this.marvelPromptBuilder = marvelPromptBuilder;
        this.messageRepository = messageRepository;
    }

    public String buildPrompt(String question, String ragContext, Long conversationId) {
        String historyContext = buildHistoryContext(conversationId);
        String combinedContext = combineContexts(historyContext, ragContext);
        return marvelPromptBuilder.buildPromptWithContext(question, combinedContext);
    }

    public String buildSimplePrompt(String question) {
        return marvelPromptBuilder.buildSimplePrompt(question);
    }

    private String buildHistoryContext(Long conversationId) {
        if (conversationId == null) {
            return "";
        }
        try {
            List<Message> messages = messageRepository
                    .findByConversationIdOrderByTimestampAsc(conversationId);

            if (messages.isEmpty()) {
                return "";
            }

            int start = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
            List<Message> recent = messages.subList(start, messages.size());

            StringBuilder sb = new StringBuilder();
            sb.append("--- Historico da conversa ---\n");
            for (Message msg : recent) {
                String role = msg.getRole() == com.project.chat.entity.MessageRole.USER ? "Usuario" : "Assistente";
                sb.append(role).append(": ").append(msg.getContent()).append("\n");
            }
            sb.append("--- Fim do historico ---");

            return sb.toString();
        } catch (Exception e) {
            log.warn("Erro ao buscar historico da conversa {}: {}", conversationId, e.getMessage());
            return "";
        }
    }

    private String combineContexts(String historyContext, String ragContext) {
        StringBuilder sb = new StringBuilder();
        if (!historyContext.isBlank()) {
            sb.append(historyContext);
        }
        if (!ragContext.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("--- Conhecimento dos documentos ---\n");
            sb.append(ragContext);
        }
        return sb.toString();
    }
}

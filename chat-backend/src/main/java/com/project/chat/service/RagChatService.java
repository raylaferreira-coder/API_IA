package com.project.chat.service;

import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.repository.AttachmentRepository;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.service.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Primary
public class RagChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);
    private static final int MAX_CONTEXT_CHUNKS = 5;

    private final ChatService chatService;
    private final AttachmentRepository attachmentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final ParserFactory parserFactory;

    public RagChatService(@Qualifier("simulatedChatService") ChatService chatService,
                          AttachmentRepository attachmentRepository,
                          DocumentChunkRepository documentChunkRepository,
                          EmbeddingService embeddingService,
                          ParserFactory parserFactory) {
        this.chatService = chatService;
        this.attachmentRepository = attachmentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
        this.parserFactory = parserFactory;
    }

    @Override
    public ChatResponse sendMessage(ChatRequest request) {
        String context = "";
        String attachmentInfo = "";

        if (request.getAttachmentId() != null) {
            attachmentInfo = processAttachment(request.getAttachmentId(), request.getContent());
            context = attachmentInfo;
        }

        String query = request.getContent();

        if (documentChunkRepository.count() > 0) {
            String ragContext = searchRelevantContext(query);
            if (!ragContext.isBlank()) {
                context = context.isBlank() ? ragContext : context + "\n\n" + ragContext;
            }
        }

        ChatRequest enrichedRequest;
        if (!context.isBlank()) {
            String enrichedContent = query + "\n\nContexto:\n" + context;
            enrichedRequest = new ChatRequest(
                    request.getSessionId(),
                    request.getConversationId(),
                    enrichedContent,
                    request.getAttachmentId()
            );
        } else {
            enrichedRequest = request;
        }

        return chatService.sendMessage(enrichedRequest);
    }

    private String processAttachment(Long attachmentId, String userQuery) {
        try {
            Optional<Attachment> optAttachment = attachmentRepository.findById(attachmentId);
            if (optAttachment.isEmpty()) {
                log.warn("Attachment não encontrado: id={}", attachmentId);
                return "";
            }

            Attachment attachment = optAttachment.get();
            String filePath = attachment.getStoragePath();
            String fileType = attachment.getFileType();

            if (filePath == null || !Files.exists(Path.of(filePath))) {
                log.warn("Arquivo não encontrado no disco: {}", filePath);
                return "";
            }

            String sourceType;
            if ("text/plain".equals(fileType)) {
                sourceType = "txt";
            } else if ("application/pdf".equals(fileType)) {
                sourceType = "pdf";
            } else {
                log.warn("Tipo de arquivo não suportado para contexto: {}", fileType);
                return "";
            }

            String text = parserFactory.getParser(sourceType).parse(filePath);

            String attachmentContent = "--- Conteúdo do arquivo anexado (" + attachment.getFileName() + ") ---\n";
            if (text.length() > 4000) {
                attachmentContent += text.substring(0, 4000) + "\n[... conteúdo truncado para contexto de chat ...]";
            } else {
                attachmentContent += text;
            }

            return attachmentContent;

        } catch (Exception e) {
            log.error("Erro ao processar attachment {}: {}", attachmentId, e.getMessage());
            return "";
        }
    }

    private String searchRelevantContext(String query) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorStr = DocumentIngestionService.toVectorString(queryEmbedding);
            List<DocumentChunk> similarChunks = documentChunkRepository.findSimilarChunks(vectorStr, MAX_CONTEXT_CHUNKS);

            if (similarChunks.isEmpty()) {
                return "";
            }

            return similarChunks.stream()
                    .map(c -> "- " + c.getContent())
                    .collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            log.warn("Erro ao buscar contexto RAG: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public HistoryResponse getHistory(String sessionId) {
        return chatService.getHistory(sessionId);
    }

    @Override
    public ConversationResponse getConversation(String sessionId, Long conversationId) {
        return chatService.getConversation(sessionId, conversationId);
    }
}

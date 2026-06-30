package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.request.UploadAndAskRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.Attachment;
import com.project.chat.entity.Conversation;
import com.project.chat.entity.Document;
import com.project.chat.entity.DocumentChunk;
import com.project.chat.entity.DocumentStatus;
import com.project.chat.entity.Message;
import com.project.chat.entity.MessageRole;
import com.project.chat.entity.Session;
import com.project.chat.exception.IngestionException;
import com.project.chat.exception.LlmServiceException;
import com.project.chat.exception.ResourceNotFoundException;
import com.project.chat.exception.SessionConflictException;
import com.project.chat.exception.ValidationException;
import com.project.chat.mapper.MessageMapper;
import com.project.chat.repository.AttachmentRepository;
import com.project.chat.repository.ConversationRepository;
import com.project.chat.repository.DocumentChunkRepository;
import com.project.chat.repository.DocumentRepository;
import com.project.chat.repository.MessageRepository;
import com.project.chat.repository.SessionRepository;
import com.project.chat.service.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Profile("rag")
public class RagChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);
    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "bmp", "tiff", "tif", "gif");
    private static final String OLLAMA_UNAVAILABLE = "O serviço de IA local está indisponível. Verifique se o Ollama está em execução.";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SessionRepository sessionRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final OllamaChatService ollamaChatService;
    private final AttachmentRepository attachmentRepository;
    private final OllamaVisionService ollamaVisionService;
    private final ParserFactory parserFactory;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkService chunkService;
    private final WebhookService webhookService;
    private final TaskService taskService;
    private final int topK;
    private final Duration readTimeout;
    private final ExecutorService streamingExecutor;
    private final ExecutorService asyncExecutor;

    public RagChatService(SessionRepository sessionRepository,
                          ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          MessageMapper messageMapper,
                          ConversationService conversationService,
                          EmbeddingService embeddingService,
                          RetrievalService retrievalService,
                          PromptBuilder promptBuilder,
                          OllamaChatService ollamaChatService,
                          AttachmentRepository attachmentRepository,
                          @Value("${rag.topK:2}") int topK,
                          OllamaVisionService ollamaVisionService,
                          ParserFactory parserFactory,
                          DocumentRepository documentRepository,
                          DocumentChunkRepository documentChunkRepository,
                          ChunkService chunkService,
                          WebhookService webhookService,
                          TaskService taskService,
                          @Value("${rag.ollama.read-timeout:300s}") Duration readTimeout) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.embeddingService = embeddingService;
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.ollamaChatService = ollamaChatService;
        this.attachmentRepository = attachmentRepository;
        this.topK = topK;
        log.info("=================================");
        log.info("RAG TOP K = {}", topK);
        log.info("=================================");
        this.ollamaVisionService = ollamaVisionService;
        this.parserFactory = parserFactory;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.chunkService = chunkService;
        this.webhookService = webhookService;
        this.taskService = taskService;
        this.streamingExecutor = Executors.newFixedThreadPool(10);
        this.asyncExecutor = Executors.newFixedThreadPool(10);
        this.readTimeout = readTimeout;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        String question = request.getContent();
        validateContent(question);

        Session session = validateSession(request.getSessionId());
        Conversation conversation = findOrCreateConversation(session, request.getConversationId(), question, null);
        Message userMessage = createAndSaveUserMessage(request, conversation);

        String answer = executeRagFlow(question);
        Message assistantMessage = saveAssistantMessage(conversation, answer);

        log.info("Mensagem RAG concluída: convId={}, userMsgId={}, assistantMsgId={}",
                conversation.getId(), userMessage.getId(), assistantMessage.getId());

        return buildChatResponse(userMessage, assistantMessage, conversation.getId());
    }

    @Override
    public String sendMessageAsync(ChatRequest request) {
        String taskId = taskService.createTask();
        log.info("Requisição async recebida: taskId={}", taskId);

        asyncExecutor.execute(() -> {
            try {
                taskService.startProcessing(taskId);
                ChatResponse result = sendMessage(request);
                taskService.complete(taskId, result);
            } catch (Exception e) {
                log.error("Erro no processamento async {}: {}", taskId, e.getMessage(), e);
                String errorMessage = OLLAMA_UNAVAILABLE;
                if (e instanceof LlmServiceException || e.getCause() instanceof LlmServiceException) {
                    errorMessage = e.getMessage();
                }
                taskService.fail(taskId, errorMessage);
            }
        });

        return taskId;
    }

    @Override
    public TaskService.TaskEntry getTaskStatus(String taskId) {
        return taskService.getTask(taskId);
    }

    @Override
    public SseEmitter sendMessageStream(ChatRequest request) {
        String question = request.getContent();
        validateContent(question);

        long emitterTimeout = readTimeout.toMillis() + 60_000;
        SseEmitter emitter = new SseEmitter(emitterTimeout);

        sendSseEvent(emitter, "connected", Map.of("type", "connected"));

        streamingExecutor.execute(() -> {
            try {
                Session session = validateSession(request.getSessionId());
                Conversation conversation = findOrCreateConversation(session, request.getConversationId(), question, null);
                Message userMessage = createAndSaveUserMessage(request, conversation);

                float[] questionVector = embeddingService.embed(question);
                List<DocumentChunk> chunks = retrievalService.search(questionVector, topK);
                String finalPrompt = promptBuilder.buildWithContext(question, chunks);

                StringBuilder fullAnswer = new StringBuilder();

                ollamaChatService.generateStream(finalPrompt,
                        token -> handleStreamToken(token, fullAnswer, emitter),
                        () -> handleStreamComplete(fullAnswer, conversation, userMessage, emitter),
                        error -> handleStreamError(error, emitter));
            } catch (Exception e) {
                log.error("Erro no sendMessageStream: {}", e.getMessage(), e);
                sendSseEvent(emitter, "error", Map.of(
                        "type", "error",
                        "content", OLLAMA_UNAVAILABLE));
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    @Transactional
    public ChatResponse uploadAndAsk(UploadAndAskRequest request) {
        Session session = validateSession(request.getSessionId());
        Conversation conversation = findOrCreateConversation(
                session, request.getConversationId(), null, "Arquivo: " + request.getOriginalFileName());

        String sourceType = detectSourceType(request.getOriginalFileName());
        boolean isImage = isImageType(sourceType);

        String question = request.getContent();
        if (question == null || question.trim().isEmpty()) {
            question = isImage ? "Descreva esta imagem em detalhes." : "Resuma este documento.";
        }

        String fileContent;
        String userMessageContent = question + "\n\n[Arquivo: " + request.getOriginalFileName() + "]";

        if (isImage) {
            fileContent = processImage(request.getStoredFilePath(), sourceType);
        } else {
            fileContent = processDocument(request.getStoredFilePath(), sourceType,
                    request.getOriginalFileName(), request.getFileSize());
        }

        Attachment attachment = createAttachment(request);
        Message userMessage = new Message(conversation, MessageRole.USER, userMessageContent);
        userMessage = messageRepository.save(userMessage);
        attachment.setMessage(userMessage);
        attachmentRepository.save(attachment);
        userMessage.setAttachment(attachment);

        String answer;
        try {
            String finalPrompt = promptBuilder.buildWithRawContext(question, fileContent);
            answer = ollamaChatService.generate(finalPrompt);
            log.info("Resposta gerada via upload-and-ask ({} caracteres)", answer.length());
        } catch (Exception e) {
            log.error("Fluxo upload-and-ask falhou: {}", e.getMessage(), e);
            throw new LlmServiceException(OLLAMA_UNAVAILABLE, e);
        }

        Message assistantMessage = saveAssistantMessage(conversation, answer);
        log.info("Upload-and-ask concluído: convId={}", conversation.getId());

        return buildChatResponse(userMessage, assistantMessage, conversation.getId());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Finalizando executors do RagChatService...");
        streamingExecutor.shutdown();
        asyncExecutor.shutdown();
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String sessionId) {
        return conversationService.getHistory(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String sessionId, Long conversationId) {
        return conversationService.getConversation(sessionId, conversationId);
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Tentativa de envio de mensagem vazia.");
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }
    }

    private Session validateSession(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada: " + sessionId));
        if (session.isExpired()) {
            throw new SessionConflictException("Sessão expirada: " + sessionId);
        }
        session.setLastActivity(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    private Conversation findOrCreateConversation(Session session, Long conversationId, String content, String titlePrefix) {
        Conversation conversation;
        if (conversationId != null) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada: " + conversationId));
            if (!conversation.getSession().getSessionId().equals(session.getSessionId())) {
                throw new ResourceNotFoundException("Conversa não encontrada: " + conversationId);
            }
        } else {
            String title;
            if (titlePrefix != null) {
                title = titlePrefix;
            } else {
                title = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            }
            conversation = new Conversation(session, title);
            conversation = conversationRepository.save(conversation);
            log.info("Nova conversa criada: id={}", conversation.getId());
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    private Message createAndSaveUserMessage(ChatRequest request, Conversation conversation) {
        Message userMessage = messageMapper.toEntity(request, conversation, MessageRole.USER);
        userMessage = messageRepository.save(userMessage);
        log.info("Mensagem do usuário salva: id={}", userMessage.getId());

        if (request.getAttachmentId() != null) {
            Attachment attachment = attachmentRepository.findById(request.getAttachmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Anexo não encontrado: " + request.getAttachmentId()));
            attachment.setMessage(userMessage);
            attachmentRepository.save(attachment);
            userMessage.setAttachment(attachment);
            log.info("Anexo vinculado à mensagem: attachmentId={}", attachment.getId());
        }

        return userMessage;
    }

    private String executeRagFlow(String question) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("========== INICIO RAG ==========");
            log.info("Pergunta: {}", question);

            float[] questionVector = embedQuestion(question);
            List<DocumentChunk> chunks = searchSimilarChunks(questionVector);
            logChunks(chunks);

            String finalPrompt = promptBuilder.buildWithContext(question, chunks);
            log.info("Prompt montado ({} caracteres): {}", finalPrompt.length(), finalPrompt);

            String answer = callOllama(finalPrompt);
            log.info("Resposta gerada via RAG ({} caracteres)", answer.length());
            log.info("========== FIM RAG ({} ms) ==========", System.currentTimeMillis() - startTime);

            return answer;
        } catch (Exception e) {
            log.error("Fluxo RAG falhou: {}", e.getMessage(), e);
            throw new LlmServiceException(OLLAMA_UNAVAILABLE, e);
        }
    }

    private float[] embedQuestion(String question) {
        long start = System.currentTimeMillis();
        float[] vector = embeddingService.embed(question);
        log.info("Embedding concluído em {} ms", System.currentTimeMillis() - start);
        return vector;
    }

    private List<DocumentChunk> searchSimilarChunks(float[] vector) {
        long start = System.currentTimeMillis();
        List<DocumentChunk> chunks = retrievalService.search(vector, topK);
        log.info("Busca vetorial concluída em {} ms ({} chunks recuperados)",
                System.currentTimeMillis() - start, chunks.size());
        return chunks;
    }

    private void logChunks(List<DocumentChunk> chunks) {
        log.info("==========================================");
        log.info("CHUNKS RECUPERADOS: {}", chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            log.info("Chunk {} - Document ID: {} - Index: {} - Texto: {}",
                    i + 1, chunk.getDocumentId(), chunk.getChunkIndex(), chunk.getContent());
        }
        log.info("==========================================");
    }

    private String callOllama(String prompt) {
        long start = System.currentTimeMillis();
        String answer = ollamaChatService.generate(prompt);
        log.info("Ollama respondeu em {} ms", System.currentTimeMillis() - start);

        if (answer == null || answer.isBlank()) {
            throw new LlmServiceException("Ollama retornou resposta vazia.");
        }
        return answer;
    }

    private Message saveAssistantMessage(Conversation conversation, String content) {
        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, content);
        assistantMessage = messageRepository.save(assistantMessage);
        log.info("Resposta do assistente salva: id={}", assistantMessage.getId());
        return assistantMessage;
    }

    private ChatResponse buildChatResponse(Message userMessage, Message assistantMessage, Long conversationId) {
        MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
        MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);
        return new ChatResponse(userMsgResponse, assistantMsgResponse, conversationId);
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(OBJECT_MAPPER.writeValueAsString(data)));
        } catch (Exception e) {
            log.error("Erro ao enviar evento SSE {}: {}", eventName, e.getMessage());
            emitter.completeWithError(e);
        }
    }

    private void handleStreamToken(String token, StringBuilder fullAnswer, SseEmitter emitter) {
        fullAnswer.append(token);
        sendSseEvent(emitter, "token", Map.of("type", "token", "content", token));
    }

    private void handleStreamComplete(StringBuilder fullAnswer, Conversation conversation,
                                      Message userMessage, SseEmitter emitter) {
        try {
            String answer = fullAnswer.toString();
            Message assistantMessage = saveAssistantMessage(conversation, answer);

            MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
            MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

            Map<String, Object> doneEvent = Map.of(
                    "type", "done",
                    "userMessage", userMsgResponse,
                    "assistantMessage", assistantMsgResponse,
                    "conversationId", conversation.getId());
            sendSseEvent(emitter, "done", doneEvent);
            emitter.complete();
        } catch (Exception e) {
            log.error("Erro ao finalizar streaming: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    private void handleStreamError(Throwable error, SseEmitter emitter) {
        log.error("Erro no streaming do Ollama: {}", error.getMessage(), error);
        sendSseEvent(emitter, "error", Map.of(
                "type", "error",
                "content", "Erro ao gerar resposta: " + error.getMessage()));
        emitter.completeWithError(error);
    }

    private Attachment createAttachment(UploadAndAskRequest request) {
        Attachment attachment = new Attachment(
                request.getOriginalFileName(),
                request.getContentType(),
                request.getFileSize(),
                request.getStoredFilePath());
        return attachmentRepository.save(attachment);
    }

    private String processImage(String filePath, String sourceType) {
        String ocrText = extractOcrText(filePath, sourceType);
        String visionDesc = describeImage(filePath);

        StringBuilder context = new StringBuilder();
        if (!ocrText.isEmpty()) {
            context.append("TEXTO EXTRAIDO DA IMAGEM:\n").append(ocrText).append("\n\n");
        }
        context.append("DESCRICAO VISUAL:\n").append(visionDesc);
        return context.toString();
    }

    private String extractOcrText(String filePath, String sourceType) {
        try (InputStream is = new FileInputStream(filePath)) {
            return parserFactory.getParser(sourceType).parse(is);
        } catch (Exception e) {
            log.warn("OCR não disponível para imagem: {}", e.getMessage());
            return "";
        }
    }

    private String describeImage(String filePath) {
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(filePath));
            return ollamaVisionService.describeImage(imageBytes);
        } catch (Exception e) {
            log.warn("Modelo de visão indisponível: {}", e.getMessage());
            return "[Descricao visual indisponivel]";
        }
    }

    private String processDocument(String filePath, String sourceType, String fileName, long fileSize) {
        Document document = new Document(fileName, filePath, sourceType);
        document.setStatus(DocumentStatus.PENDING);
        document.setFileSize(fileSize);
        document = documentRepository.save(document);

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            String rawText = parseFile(filePath, sourceType);
            ingestDocumentChunks(document, rawText);

            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Documento ingerido: id={}, chunks={}", document.getId(), document.getTotalChunks());

            webhookService.notify(document.getId(), filePath,
                    DocumentStatus.COMPLETED, document.getTotalChunks(), 0);

            return rawText;
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            log.error("Falha ao ingerir documento: {}", e.getMessage(), e);
            throw new IngestionException("Falha ao processar documento: " + e.getMessage(), e);
        }
    }

    private String parseFile(String filePath, String sourceType) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return parserFactory.getParser(sourceType).parse(inputStream);
        } catch (Exception e) {
            throw new IngestionException("Erro ao ler arquivo: " + e.getMessage(), e);
        }
    }

    private void ingestDocumentChunks(Document document, String rawText) {
        List<String> chunks = chunkService.chunkText(rawText);
        List<float[]> embeddings = embeddingService.embedAll(chunks);

        List<DocumentChunk> chunkEntities = new ArrayList<>();
        int chunksSemEmbedding = 0;
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk(
                    document, i, chunks.get(i), chunkService.estimateTokens(chunks.get(i)));
            if (i < embeddings.size()) {
                chunk.setEmbedding(embeddings.get(i));
            } else {
                chunksSemEmbedding++;
            }
            chunkEntities.add(chunk);
        }
        if (chunksSemEmbedding > 0) {
            log.warn("{} chunks salvos sem embedding para o documento {}",
                    chunksSemEmbedding, document.getId());
        }
        documentChunkRepository.saveAll(chunkEntities);
        document.setTotalChunks(chunks.size());
    }

    private String detectSourceType(String fileName) {
        if (fileName == null) return "txt";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "markdown";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "html";
        if (lower.endsWith(".docx")) return "docx";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg";
        if (lower.endsWith(".png")) return "png";
        if (lower.endsWith(".bmp")) return "bmp";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "tiff";
        if (lower.endsWith(".gif")) return "gif";
        return "txt";
    }

    private boolean isImageType(String sourceType) {
        return IMAGE_TYPES.contains(sourceType.toLowerCase());
    }
}

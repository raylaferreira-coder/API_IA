package com.project.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chat.dto.request.ChatRequest;
import com.project.chat.dto.request.UploadAndAskRequest;
import com.project.chat.dto.response.ChatResponse;
import com.project.chat.dto.response.ConversationResponse;
import com.project.chat.dto.response.HistoryResponse;
import com.project.chat.dto.response.MessageResponse;
import com.project.chat.entity.*;
import com.project.chat.exception.*;
import com.project.chat.mapper.MessageMapper;
import com.project.chat.repository.*;
import com.project.chat.service.parser.ParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.FileInputStream;
import java.io.IOException;
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

    private final int topK;

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
    private final ExecutorService streamingExecutor;
    private final ExecutorService asyncExecutor;
    private final ObjectMapper objectMapper;
    private final Duration readTimeout;
    private final TaskService taskService;

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
                          @Value("${rag.topK:5}") int topK,
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
        this.ollamaVisionService = ollamaVisionService;
        this.parserFactory = parserFactory;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.chunkService = chunkService;
        this.webhookService = webhookService;
        this.taskService = taskService;
        this.streamingExecutor = Executors.newFixedThreadPool(10);
        this.asyncExecutor = Executors.newFixedThreadPool(10);
        this.objectMapper = new ObjectMapper();
        this.readTimeout = readTimeout;
    }

    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        String content = request.getContent();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Tentativa de envio de mensagem vazia.");
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }

        Session session = sessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + request.getSessionId()));

        if (session.isExpired()) {
            throw new SessionConflictException("Sessão expirada: " + request.getSessionId());
        }

        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);

        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversa não encontrada: " + request.getConversationId()));
            if (!conversation.getSession().getSessionId().equals(request.getSessionId())) {
                throw new ResourceNotFoundException(
                        "Conversa não encontrada: " + request.getConversationId());
            }
        } else {
            String title = content.length() > 50
                    ? content.substring(0, 50) + "..."
                    : content;
            conversation = new Conversation(session, title);
            conversation = conversationRepository.save(conversation);
            log.info("Nova conversa criada: id={}", conversation.getId());
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

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

        // RAG flow: embedding → retrieval → prompt builder → Ollama → response
        String answer;
        try {
            float[] questionVector = embeddingService.embed(content);
            List<DocumentChunk> chunks = retrievalService.search(questionVector, topK);
            String finalPrompt = promptBuilder.buildWithContext(content, chunks);
            answer = ollamaChatService.generate(finalPrompt);
            if (answer == null) {
                throw new LlmServiceException("Ollama retornou resposta vazia.");
            }
            log.info("Resposta gerada via RAG ({} caracteres)", answer.length());
        } catch (Exception e) {
            log.error("Fluxo RAG falhou: {}", e.getMessage(), e);
            throw new LlmServiceException("O serviço de IA local está indisponível. Verifique se o Ollama está em execução.", e);
        }

        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, answer);
        assistantMessage = messageRepository.save(assistantMessage);
        log.info("Resposta do assistente salva: id={}", assistantMessage.getId());

        MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
        MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

        return new ChatResponse(userMsgResponse, assistantMsgResponse, conversation.getId());
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
                String msg = "O serviço de IA local está indisponível. Verifique se o Ollama está em execução.";
                if (e instanceof LlmServiceException || e.getCause() instanceof LlmServiceException) {
                    msg = e.getMessage();
                }
                taskService.fail(taskId, msg);
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
        String content = request.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("A mensagem não pode conter apenas espaços em branco.");
        }

        long emitterTimeout = readTimeout.toMillis() + 60_000;
        SseEmitter emitter = new SseEmitter(emitterTimeout);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"type\":\"connected\"}"));
        } catch (IOException e) {
            log.error("Erro ao enviar heartbeat inicial: {}", e.getMessage());
            emitter.completeWithError(e);
            return emitter;
        }

        streamingExecutor.execute(() -> {
            try {
                Session session = sessionRepository.findBySessionId(request.getSessionId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Sessão não encontrada: " + request.getSessionId()));

                if (session.isExpired()) {
                    throw new SessionConflictException("Sessão expirada: " + request.getSessionId());
                }

                session.setLastActivity(LocalDateTime.now());
                sessionRepository.save(session);

                Conversation conversation;
                if (request.getConversationId() != null) {
                    conversation = conversationRepository.findById(request.getConversationId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Conversa não encontrada: " + request.getConversationId()));
                    if (!conversation.getSession().getSessionId().equals(request.getSessionId())) {
                        throw new ResourceNotFoundException(
                                "Conversa não encontrada: " + request.getConversationId());
                    }
                } else {
                    String title = content.length() > 50
                            ? content.substring(0, 50) + "..."
                            : content;
                    conversation = new Conversation(session, title);
                    conversation = conversationRepository.save(conversation);
                    log.info("Nova conversa criada via streaming: id={}", conversation.getId());
                }

                conversation.setUpdatedAt(LocalDateTime.now());
                conversationRepository.save(conversation);

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
                }

                float[] questionVector = embeddingService.embed(content);
                List<DocumentChunk> chunks = retrievalService.search(questionVector, topK);
                String finalPrompt = promptBuilder.buildWithContext(content, chunks);

                Conversation capturedConversation = conversation;
                Message capturedUserMessage = userMessage;
                StringBuilder fullAnswer = new StringBuilder();

                ollamaChatService.generateStream(finalPrompt,
                    token -> {
                        fullAnswer.append(token);
                        try {
                            Map<String, String> event = Map.of("type", "token", "content", token);
                            emitter.send(SseEmitter.event()
                                    .name("token")
                                    .data(objectMapper.writeValueAsString(event)));
                        } catch (Exception e) {
                            log.error("Erro ao enviar token SSE: {}", e.getMessage());
                        }
                    },
                    () -> {
                        try {
                            String answer = fullAnswer.toString();
                            Message assistantMessage = new Message(capturedConversation, MessageRole.ASSISTANT, answer);
                            assistantMessage = messageRepository.save(assistantMessage);
                            log.info("Resposta completa salva via streaming: id={}", assistantMessage.getId());

                            MessageResponse userMsgResponse = messageMapper.toResponse(capturedUserMessage);
                            MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

                            Map<String, Object> doneEvent = Map.of(
                                    "type", "done",
                                    "userMessage", userMsgResponse,
                                    "assistantMessage", assistantMsgResponse,
                                    "conversationId", capturedConversation.getId()
                            );
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(objectMapper.writeValueAsString(doneEvent)));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("Erro ao finalizar streaming: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        log.error("Erro no streaming do Ollama: {}", error.getMessage());
                        try {
                            Map<String, String> errorEvent = Map.of(
                                    "type", "error",
                                    "content", "Erro ao gerar resposta: " + error.getMessage()
                            );
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(objectMapper.writeValueAsString(errorEvent)));
                        } catch (IOException ex) {
                            log.error("Erro ao enviar evento de erro SSE: {}", ex.getMessage());
                        }
                        emitter.completeWithError(error);
                    }
                );
            } catch (Exception e) {
                log.error("Erro no sendMessageStream: {}", e.getMessage(), e);
                try {
                    Map<String, String> errorEvent = Map.of(
                            "type", "error",
                            "content", "O serviço de IA local está indisponível. Verifique se o Ollama está em execução."
                    );
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(errorEvent)));
                } catch (IOException ex) {
                    log.error("Erro ao enviar evento de erro SSE: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    @Transactional
    public ChatResponse uploadAndAsk(UploadAndAskRequest request) {
        Session session = sessionRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sessão não encontrada: " + request.getSessionId()));

        if (session.isExpired()) {
            throw new SessionConflictException("Sessão expirada: " + request.getSessionId());
        }

        session.setLastActivity(LocalDateTime.now());
        sessionRepository.save(session);

        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversa não encontrada: " + request.getConversationId()));
            if (!conversation.getSession().getSessionId().equals(request.getSessionId())) {
                throw new ResourceNotFoundException(
                        "Conversa não encontrada: " + request.getConversationId());
            }
        } else {
            String title = "Arquivo: " + request.getOriginalFileName();
            conversation = new Conversation(session, title);
            conversation = conversationRepository.save(conversation);
            log.info("Nova conversa criada via upload: id={}", conversation.getId());
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        String sourceType = detectSourceType(request.getOriginalFileName());
        boolean isImage = isImageType(sourceType);

        String question = request.getContent();
        if (question == null || question.trim().isEmpty()) {
            question = isImage
                    ? "Descreva esta imagem em detalhes."
                    : "Resuma este documento.";
        }

        String fileContent;
        String userMessageContent = question + "\n\n[Arquivo: " + request.getOriginalFileName() + "]";

        if (isImage) {
            fileContent = processImage(request.getStoredFilePath(), sourceType);
        } else {
            fileContent = processDocument(request.getStoredFilePath(), sourceType,
                    request.getOriginalFileName(), request.getFileSize());
        }

        Attachment attachment = new Attachment(
                request.getOriginalFileName(),
                request.getContentType(),
                request.getFileSize(),
                request.getStoredFilePath()
        );
        attachment = attachmentRepository.save(attachment);

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
            throw new LlmServiceException("O serviço de IA local está indisponível. Verifique se o Ollama está em execução.", e);
        }

        Message assistantMessage = new Message(conversation, MessageRole.ASSISTANT, answer);
        assistantMessage = messageRepository.save(assistantMessage);
        log.info("Resposta do assistente salva via upload: id={}", assistantMessage.getId());

        MessageResponse userMsgResponse = messageMapper.toResponse(userMessage);
        MessageResponse assistantMsgResponse = messageMapper.toResponse(assistantMessage);

        return new ChatResponse(userMsgResponse, assistantMsgResponse, conversation.getId());
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

    private String processImage(String filePath, String sourceType) {
        String ocrText = "";
        try (InputStream is = new FileInputStream(filePath)) {
            ocrText = parserFactory.getParser(sourceType).parse(is);
        } catch (Exception e) {
            log.warn("OCR nao disponivel para imagem: {}", e.getMessage());
        }

        String visionDesc;
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(filePath));
            visionDesc = ollamaVisionService.describeImage(imageBytes);
        } catch (Exception e) {
            log.warn("Modelo de visao indisponivel: {}", e.getMessage());
            visionDesc = "[Descricao visual indisponivel]";
        }

        StringBuilder ctx = new StringBuilder();
        if (!ocrText.isEmpty()) {
            ctx.append("TEXTO EXTRAIDO DA IMAGEM:\n").append(ocrText).append("\n\n");
        }
        ctx.append("DESCRICAO VISUAL:\n").append(visionDesc);
        return ctx.toString();
    }

    private String processDocument(String filePath, String sourceType, String fileName, long fileSize) {
        Document document = new Document(fileName, filePath, sourceType);
        document.setStatus(DocumentStatus.PENDING);
        document.setFileSize(fileSize);
        document = documentRepository.save(document);

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            String rawText;
            try (InputStream inputStream = new FileInputStream(filePath)) {
                rawText = parserFactory.getParser(sourceType).parse(inputStream);
            }

            List<String> chunksList = chunkService.chunkText(rawText);
            List<float[]> embeddings = embeddingService.embedAll(chunksList);

            List<DocumentChunk> chunkEntities = new ArrayList<>();
            int chunksSemEmbedding = 0;
            for (int i = 0; i < chunksList.size(); i++) {
                DocumentChunk chunk = new DocumentChunk(
                        document, i, chunksList.get(i), chunkService.estimateTokens(chunksList.get(i)));
                if (i < embeddings.size()) {
                    chunk.setEmbedding(embeddings.get(i));
                } else {
                    chunksSemEmbedding++;
                }
                chunkEntities.add(chunk);
            }
            if (chunksSemEmbedding > 0) {
                log.warn("{} chunks salvos sem embedding para o documento {}", chunksSemEmbedding, document.getId());
            }
            documentChunkRepository.saveAll(chunkEntities);

            document.setTotalChunks(chunksList.size());
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);
            log.info("Documento ingerido: id={}, chunks={}", document.getId(), chunksList.size());

            webhookService.notify(document.getId(), filePath,
                    DocumentStatus.COMPLETED, chunksList.size(), 0);

            return rawText;
        } catch (Exception e) {
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            log.error("Falha ao ingerir documento: {}", e.getMessage());
            throw new IngestionException("Falha ao processar documento: " + e.getMessage(), e);
        }
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

package com.project.chat.service;

import com.project.chat.dto.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ConcurrentHashMap<String, TaskEntry> tasks = new ConcurrentHashMap<>();

    public String createTask() {
        String taskId = java.util.UUID.randomUUID().toString();
        TaskEntry entry = new TaskEntry();
        entry.status = "PENDING";
        entry.createdAt = LocalDateTime.now().format(FMT);
        tasks.put(taskId, entry);
        log.info("Tarefa criada: taskId={}", taskId);
        return taskId;
    }

    public void startProcessing(String taskId) {
        TaskEntry entry = tasks.get(taskId);
        if (entry != null) {
            entry.status = "PROCESSING";
        }
    }

    public void complete(String taskId, ChatResponse result) {
        TaskEntry entry = tasks.get(taskId);
        if (entry != null) {
            entry.status = "COMPLETED";
            entry.result = result;
            entry.completedAt = LocalDateTime.now().format(FMT);
            log.info("Tarefa concluída: taskId={}", taskId);
        }
    }

    public void fail(String taskId, String error) {
        TaskEntry entry = tasks.get(taskId);
        if (entry != null) {
            entry.status = "FAILED";
            entry.errorMessage = error;
            entry.completedAt = LocalDateTime.now().format(FMT);
            log.error("Tarefa falhou: taskId={}, erro={}", taskId, error);
        }
    }

    public TaskEntry getTask(String taskId) {
        return tasks.get(taskId);
    }

    public static class TaskEntry {
        private String status;
        private ChatResponse result;
        private String errorMessage;
        private String createdAt;
        private String completedAt;

        public String getStatus() { return status; }
        public ChatResponse getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public String getCreatedAt() { return createdAt; }
        public String getCompletedAt() { return completedAt; }
    }
}

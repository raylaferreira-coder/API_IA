package com.project.chat.dto.response;

public class TaskStatusResponse {

    private String taskId;
    private String status;
    private ChatResponse result;
    private String errorMessage;
    private String createdAt;
    private String completedAt;

    public TaskStatusResponse() {
    }

    public TaskStatusResponse(String taskId, String status, String createdAt) {
        this.taskId = taskId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ChatResponse getResult() {
        return result;
    }

    public void setResult(ChatResponse result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}

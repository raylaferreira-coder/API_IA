package com.project.chat.ai.dto;

public class EmbeddingRequest {

    private String model;
    private String prompt;

    public EmbeddingRequest() {
    }

    public EmbeddingRequest(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}

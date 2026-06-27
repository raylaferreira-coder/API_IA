package com.project.chat.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddingResponse {

    private float[] embedding;
    @JsonProperty("prompt_eval_count")
    private int promptEvalCount;
    @JsonProperty("load_duration")
    private long loadDuration;
    @JsonProperty("total_duration")
    private long totalDuration;

    public EmbeddingResponse() {
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public int getPromptEvalCount() {
        return promptEvalCount;
    }

    public void setPromptEvalCount(int promptEvalCount) {
        this.promptEvalCount = promptEvalCount;
    }

    public long getLoadDuration() {
        return loadDuration;
    }

    public void setLoadDuration(long loadDuration) {
        this.loadDuration = loadDuration;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }
}

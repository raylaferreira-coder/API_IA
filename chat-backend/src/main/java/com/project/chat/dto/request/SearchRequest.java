package com.project.chat.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "A consulta é obrigatória.")
    private String query;

    @Min(value = 1, message = "O limite mínimo é 1.")
    @Max(value = 100, message = "O limite máximo é 100.")
    private int topK = 1;

    public SearchRequest() {
    }

    public SearchRequest(String query, int topK) {
        this.query = query;
        this.topK = topK;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}

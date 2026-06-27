package com.project.chat.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "A consulta é obrigatória.")
    private String query;

    @Min(value = 1, message = "O limite mínimo é 1.")
    @Max(value = 100, message = "O limite máximo é 100.")
    private int limit = 5;

    public SearchRequest() {}

    public SearchRequest(String query, int limit) {
        this.query = query;
        this.limit = limit;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}

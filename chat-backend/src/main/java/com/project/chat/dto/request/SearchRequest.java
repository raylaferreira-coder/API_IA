package com.project.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SearchRequest {

    @NotBlank(message = "A consulta é obrigatória.")
    private String query;

    private Integer limit = 5;

    public SearchRequest() {}

    public SearchRequest(String query, Integer limit) {
        this.query = query;
        this.limit = limit;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getLimit() { return limit != null ? limit : 5; }
    public void setLimit(Integer limit) { this.limit = limit; }
}

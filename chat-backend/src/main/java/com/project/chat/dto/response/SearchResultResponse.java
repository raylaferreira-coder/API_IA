package com.project.chat.dto.response;

import java.util.List;

public class SearchResultResponse {

    private String query;
    private int totalResults;
    private List<DocumentChunkResponse> results;

    public SearchResultResponse() {}

    public SearchResultResponse(String query, int totalResults, List<DocumentChunkResponse> results) {
        this.query = query;
        this.totalResults = totalResults;
        this.results = results;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public List<DocumentChunkResponse> getResults() { return results; }
    public void setResults(List<DocumentChunkResponse> results) { this.results = results; }
}

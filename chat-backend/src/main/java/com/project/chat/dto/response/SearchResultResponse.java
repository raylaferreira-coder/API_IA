package com.project.chat.dto.response;

import java.util.List;

public class SearchResultResponse {

    private List<DocumentChunkResponse> results;

    public SearchResultResponse() {}

    public SearchResultResponse(List<DocumentChunkResponse> results) {
        this.results = results;
    }

    public List<DocumentChunkResponse> getResults() { return results; }
    public void setResults(List<DocumentChunkResponse> results) { this.results = results; }
}

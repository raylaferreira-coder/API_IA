package com.project.chat.dto.response;

import java.util.List;

public class MarvelIngestionResponse {

    private String source;
    private boolean configured;
    private int documents;
    private int chunks;
    private int errors;
    private long elapsedMs;
    private List<MarvelSourceResult> sources;

    public MarvelIngestionResponse() {}

    public MarvelIngestionResponse(String source, boolean configured, int documents, int chunks, int errors, long elapsedMs) {
        this.source = source;
        this.configured = configured;
        this.documents = documents;
        this.chunks = chunks;
        this.errors = errors;
        this.elapsedMs = elapsedMs;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }

    public int getDocuments() { return documents; }
    public void setDocuments(int documents) { this.documents = documents; }

    public int getChunks() { return chunks; }
    public void setChunks(int chunks) { this.chunks = chunks; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    public List<MarvelSourceResult> getSources() { return sources; }
    public void setSources(List<MarvelSourceResult> sources) { this.sources = sources; }

    public static class MarvelSourceResult {
        private String source;
        private int documents;
        private int chunks;
        private int errors;
        private long elapsedMs;

        public MarvelSourceResult() {}

        public MarvelSourceResult(String source, int documents, int chunks, int errors, long elapsedMs) {
            this.source = source;
            this.documents = documents;
            this.chunks = chunks;
            this.errors = errors;
            this.elapsedMs = elapsedMs;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public int getDocuments() { return documents; }
        public void setDocuments(int documents) { this.documents = documents; }

        public int getChunks() { return chunks; }
        public void setChunks(int chunks) { this.chunks = chunks; }

        public int getErrors() { return errors; }
        public void setErrors(int errors) { this.errors = errors; }

        public long getElapsedMs() { return elapsedMs; }
        public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    }
}

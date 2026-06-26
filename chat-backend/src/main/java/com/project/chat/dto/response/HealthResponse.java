package com.project.chat.dto.response;

import java.time.LocalDateTime;

public class HealthResponse {

    private String status;
    private String database;
    private String diskSpace;
    private LocalDateTime timestamp;
    private String version;

    public HealthResponse() {
    }

    public HealthResponse(String status, String database, String diskSpace, LocalDateTime timestamp, String version) {
        this.status = status;
        this.database = database;
        this.diskSpace = diskSpace;
        this.timestamp = timestamp;
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getDiskSpace() {
        return diskSpace;
    }

    public void setDiskSpace(String diskSpace) {
        this.diskSpace = diskSpace;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

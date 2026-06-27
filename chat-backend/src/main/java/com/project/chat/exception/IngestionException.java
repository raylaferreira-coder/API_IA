package com.project.chat.exception;

public class IngestionException extends RuntimeException {
    public IngestionException(String message) {
        super(message);
    }
    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}

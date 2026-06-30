package com.project.chat.exception;

public class SessionConflictException extends RuntimeException {
    public SessionConflictException(String message) {
        super(message);
    }
}

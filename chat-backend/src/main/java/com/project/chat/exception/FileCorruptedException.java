package com.project.chat.exception;

public class FileCorruptedException extends RuntimeException {

    public FileCorruptedException(String message) {
        super(message);
    }

    public FileCorruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}

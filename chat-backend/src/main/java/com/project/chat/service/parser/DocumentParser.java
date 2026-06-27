package com.project.chat.service.parser;

public interface DocumentParser {
    String parse(String source) throws Exception;
    boolean supports(String sourceType);
}

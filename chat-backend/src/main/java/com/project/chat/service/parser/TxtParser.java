package com.project.chat.service.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TxtParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(TxtParser.class);

    @Override
    public String parse(String filePath) throws Exception {
        log.info("Lendo arquivo TXT: {}", filePath);
        String content = Files.readString(Path.of(filePath));
        log.info("TXT lido: {} caracteres", content.length());
        return content;
    }

    @Override
    public boolean supports(String sourceType) {
        return "txt".equalsIgnoreCase(sourceType) || "text/plain".equalsIgnoreCase(sourceType);
    }
}

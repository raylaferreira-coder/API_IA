package com.project.chat.service.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class TxtParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(TxtParser.class);

    @Override
    public String parse(InputStream inputStream) throws IOException {
        log.info("Lendo TXT a partir de InputStream");
        String content = new String(inputStream.readAllBytes());
        log.info("TXT lido: {} caracteres", content.length());
        return content;
    }

    @Override
    public boolean supports(String sourceType) {
        return "txt".equalsIgnoreCase(sourceType) || "text/plain".equalsIgnoreCase(sourceType);
    }
}

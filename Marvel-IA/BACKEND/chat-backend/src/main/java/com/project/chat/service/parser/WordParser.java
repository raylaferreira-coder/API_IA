package com.project.chat.service.parser;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class WordParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(WordParser.class);

    @Override
    public String parse(InputStream inputStream) throws IOException {
        log.info("Lendo DOCX a partir de InputStream");
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String content = extractor.getText();
            log.info("DOCX lido: {} caracteres", content.length());
            return content;
        }
    }

    @Override
    public boolean supports(String sourceType) {
        return "docx".equalsIgnoreCase(sourceType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(sourceType);
    }
}

package com.project.chat.service.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class MarkdownParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(MarkdownParser.class);

    @Override
    public String parse(InputStream inputStream) throws IOException {
        log.info("Lendo Markdown a partir de InputStream");
        String content = new String(inputStream.readAllBytes());
        String stripped = stripMarkdown(content);
        log.info("Markdown lido: {} caracteres (após strip)", stripped.length());
        return stripped;
    }

    @Override
    public boolean supports(String sourceType) {
        return "markdown".equalsIgnoreCase(sourceType) || "md".equalsIgnoreCase(sourceType)
                || "text/markdown".equalsIgnoreCase(sourceType);
    }

    private String stripMarkdown(String markdown) {
        return markdown
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("`{1,3}[^`]*`{1,3}", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "")
                .replaceAll("(?m)^[\\s]*[-*+][\\s]+", "")
                .replaceAll("(?m)^[\\s]*\\d+\\.[\\s]+", "")
                .replaceAll("(?m)^\\|(.+)\\|$", "$1")
                .replaceAll("[-]{3,}", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}

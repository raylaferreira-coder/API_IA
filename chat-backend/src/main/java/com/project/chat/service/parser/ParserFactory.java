package com.project.chat.service.parser;

import com.project.chat.exception.IngestionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParserFactory {

    private static final Logger log = LoggerFactory.getLogger(ParserFactory.class);

    private final List<DocumentParser> parsers;

    public ParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
        log.info("Parsers registrados: {}", parsers.stream().map(p -> p.getClass().getSimpleName()).toList());
    }

    public DocumentParser getParser(String sourceType) {
        return parsers.stream()
                .filter(p -> p.supports(sourceType))
                .findFirst()
                .orElseThrow(() -> new IngestionException(
                        "Nenhum parser encontrado para o tipo: " + sourceType));
    }
}

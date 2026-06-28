package com.project.chat.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);

    @Override
    public String parse(InputStream inputStream) throws IOException {
        log.info("Lendo PDF a partir de InputStream");
        byte[] bytes = inputStream.readAllBytes();
        try {
            return extractText(bytes);
        } catch (IOException e) {
            throw new com.project.chat.exception.FileCorruptedException("PDF corrompido ou inválido: " + e.getMessage(), e);
        }
    }

    private String extractText(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String content = stripper.getText(document);
            log.info("PDF lido: {} páginas, {} caracteres", document.getNumberOfPages(), content.length());
            return content;
        }
    }

    @Override
    public boolean supports(String sourceType) {
        return "pdf".equalsIgnoreCase(sourceType) || "application/pdf".equalsIgnoreCase(sourceType);
    }
}

package com.project.chat.service.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class PdfParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);

    @Override
    public String parse(String filePath) throws Exception {
        log.info("Lendo arquivo PDF: {}", filePath);
        try (PDDocument document = Loader.loadPDF(Path.of(filePath).toFile())) {
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

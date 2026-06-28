package com.project.chat.util;

import com.project.chat.service.parser.PdfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Legacy adapter — substituído por {@link PdfParser} com Apache PDFBox.
 * Mantido para compatibilidade com código legado.
 */
@Deprecated
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    private final PdfParser pdfParser;

    public PdfTextExtractor() {
        this.pdfParser = new PdfParser();
        log.warn("PdfTextExtractor está obsoleto. Utilize PdfParser diretamente.");
    }

    public String extractText(byte[] pdfBytes) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
            return pdfParser.parse(inputStream);
        }
    }

    public String extractText(InputStream inputStream) throws IOException {
        return pdfParser.parse(inputStream);
    }
}

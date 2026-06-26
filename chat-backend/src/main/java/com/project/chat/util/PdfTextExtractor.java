package com.project.chat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    public static String extractText(InputStream inputStream) throws IOException {
        log.info("Extraindo texto de arquivo PDF...");
        StringBuilder text = new StringBuilder();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            text.append(new String(buffer, 0, bytesRead));
        }
        String result = text.toString();
        if (result.isBlank()) {
            log.warn("Nenhum texto extraído do PDF.");
        }
        return result;
    }

    private PdfTextExtractor() {
    }
}

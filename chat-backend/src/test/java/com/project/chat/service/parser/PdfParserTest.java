package com.project.chat.service.parser;

import com.project.chat.exception.FileCorruptedException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfParserTest {

    private final PdfParser parser = new PdfParser();

    @Test
    void parse_WithValidPdf_ShouldExtractText() throws Exception {
        byte[] pdfBytes = createMinimalPdf("Conteudo PDF de teste");

        String result = parser.parse(new ByteArrayInputStream(pdfBytes));

        assertTrue(result.contains("Conteudo PDF de teste"));
    }

    @Test
    void parse_WithMultilinePdf_ShouldExtractAllText() throws Exception {
        byte[] pdfBytes = createMinimalPdf("Linha 1\nLinha 2\nLinha 3");

        String result = parser.parse(new ByteArrayInputStream(pdfBytes));

        assertTrue(result.contains("Linha 1"));
        assertTrue(result.contains("Linha 2"));
        assertTrue(result.contains("Linha 3"));
    }

    @Test
    void parse_WithCorruptedPdf_ShouldThrowFileCorruptedException() {
        byte[] invalidPdf = {0x25, 0x50, 0x44, 0x46, (byte) 0xFF, (byte) 0xFF};

        assertThrows(FileCorruptedException.class,
                () -> parser.parse(new ByteArrayInputStream(invalidPdf)));
    }

    @Test
    void supports_WithPdfExtension_ShouldReturnTrue() {
        assertTrue(parser.supports("pdf"));
    }

    @Test
    void supports_WithApplicationPdfMime_ShouldReturnTrue() {
        assertTrue(parser.supports("application/pdf"));
    }

    @Test
    void supports_WithTxtExtension_ShouldReturnFalse() {
        assertFalse(parser.supports("txt"));
    }

    private byte[] createMinimalPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                for (String line : text.split("\n")) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -14);
                }
                contentStream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}

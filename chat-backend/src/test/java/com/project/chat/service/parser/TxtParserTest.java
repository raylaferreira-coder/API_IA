package com.project.chat.service.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TxtParserTest {

    private final TxtParser parser = new TxtParser();

    @Test
    void parse_WithSimpleText_ShouldReturnContent() throws Exception {
        String expected = "Conteudo de teste";
        InputStream input = new ByteArrayInputStream(expected.getBytes());

        String result = parser.parse(input);

        assertEquals(expected, result);
    }

    @Test
    void parse_WithMultilineText_ShouldPreserveLines() throws Exception {
        String expected = "Linha 1\nLinha 2\nLinha 3";
        InputStream input = new ByteArrayInputStream(expected.getBytes());

        String result = parser.parse(input);

        assertEquals(expected, result);
    }

    @Test
    void parse_WithEmptyContent_ShouldReturnEmptyString() throws Exception {
        InputStream input = new ByteArrayInputStream(new byte[0]);

        String result = parser.parse(input);

        assertEquals("", result);
    }

    @Test
    void supports_WithTxtExtension_ShouldReturnTrue() {
        assertTrue(parser.supports("txt"));
    }

    @Test
    void supports_WithTextPlainMime_ShouldReturnTrue() {
        assertTrue(parser.supports("text/plain"));
    }

    @Test
    void supports_WithPdfExtension_ShouldReturnFalse() {
        assertFalse(parser.supports("pdf"));
    }
}

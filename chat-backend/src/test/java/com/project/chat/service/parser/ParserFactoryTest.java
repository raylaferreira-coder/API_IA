package com.project.chat.service.parser;

import com.project.chat.exception.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserFactoryTest {

    private final TxtParser txtParser = new TxtParser();
    private final PdfParser pdfParser = new PdfParser();
    private final HtmlParser htmlParser = new HtmlParser();
    private final MarkdownParser markdownParser = new MarkdownParser();

    @Test
    void getParser_WithTxtExtension_ShouldReturnTxtParser() {
        ParserFactory factory = new ParserFactory(List.of(txtParser, pdfParser));

        DocumentParser result = factory.getParser("txt");

        assertInstanceOf(TxtParser.class, result);
    }

    @Test
    void getParser_WithPdfExtension_ShouldReturnPdfParser() {
        ParserFactory factory = new ParserFactory(List.of(txtParser, pdfParser));

        DocumentParser result = factory.getParser("pdf");

        assertInstanceOf(PdfParser.class, result);
    }

    @Test
    void getParser_WithHtmlExtension_ShouldReturnHtmlParser() {
        ParserFactory factory = new ParserFactory(List.of(txtParser, pdfParser, htmlParser));

        DocumentParser result = factory.getParser("html");

        assertInstanceOf(HtmlParser.class, result);
    }

    @Test
    void getParser_WithMdExtension_ShouldReturnMarkdownParser() {
        ParserFactory factory = new ParserFactory(List.of(txtParser, pdfParser, markdownParser));

        DocumentParser result = factory.getParser("md");

        assertInstanceOf(MarkdownParser.class, result);
    }

    @Test
    void getParser_WithUnsupportedExtension_ShouldThrowUnsupportedFileTypeException() {
        ParserFactory factory = new ParserFactory(List.of(txtParser, pdfParser));

        assertThrows(UnsupportedFileTypeException.class,
                () -> factory.getParser("docx"));
    }

    @Test
    void getParser_WithEmptyParserList_ShouldThrowUnsupportedFileTypeException() {
        ParserFactory factory = new ParserFactory(List.of());

        assertThrows(UnsupportedFileTypeException.class,
                () -> factory.getParser("txt"));
    }
}

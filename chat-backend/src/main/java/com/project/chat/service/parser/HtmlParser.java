package com.project.chat.service.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class HtmlParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(HtmlParser.class);

    @Override
    public String parse(InputStream inputStream) throws IOException {
        log.info("Lendo HTML a partir de InputStream");
        Document doc = Jsoup.parse(inputStream, "UTF-8", "");
        doc.select("script, style, nav, footer, header, aside, .sidebar, .nav, .footer, .header, .menu, .ads, .advertisement").remove();
        StringBuilder sb = new StringBuilder();
        String title = doc.title();
        if (!title.isEmpty()) {
            sb.append("Título: ").append(title).append("\n\n");
        }
        for (Element element : doc.select("h1, h2, h3, h4, h5, h6, p, li, td, th, blockquote, pre")) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                if (element.tagName().startsWith("h")) {
                    sb.append("\n").append(text).append("\n");
                } else {
                    sb.append(text).append("\n");
                }
            }
        }
        String result = sb.toString().trim();
        log.info("HTML lido: {} caracteres", result.length());
        return result;
    }

    @Override
    public boolean supports(String sourceType) {
        return "html".equalsIgnoreCase(sourceType) || "htm".equalsIgnoreCase(sourceType)
                || "text/html".equalsIgnoreCase(sourceType);
    }
}

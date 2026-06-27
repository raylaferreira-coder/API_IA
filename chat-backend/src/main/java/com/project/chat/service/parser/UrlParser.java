package com.project.chat.service.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UrlParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(UrlParser.class);

    @Override
    public String parse(String url) throws Exception {
        log.info("Baixando conteúdo da URL: {}", url);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; MarvelRAG/1.0)")
                .timeout(30000)
                .get();

        doc.select("script, style, nav, footer, header, aside, .sidebar, .nav, .footer, .header, .menu, .ads, .advertisement").remove();

        String title = doc.title();
        StringBuilder sb = new StringBuilder();
        sb.append("Título: ").append(title).append("\n\n");

        for (Element element : doc.select("h1, h2, h3, h4, h5, h6, p, li, td, th, blockquote, pre")) {
            String tagName = element.tagName();
            String text = element.text().trim();
            if (!text.isEmpty()) {
                if (tagName.startsWith("h")) {
                    sb.append("\n").append(text).append("\n");
                } else {
                    sb.append(text).append("\n");
                }
            }
        }

        String result = sb.toString().trim();
        log.info("URL processada: {} caracteres extraídos", result.length());
        return result;
    }

    @Override
    public boolean supports(String sourceType) {
        return "url".equalsIgnoreCase(sourceType) || "web".equalsIgnoreCase(sourceType);
    }
}

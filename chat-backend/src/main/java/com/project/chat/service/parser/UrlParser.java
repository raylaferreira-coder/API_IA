package com.project.chat.service.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Component
public class UrlParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(UrlParser.class);

    @Override
    public String parse(InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException("UrlParser não suporta parsing por InputStream. Use parseUrl(String).");
    }

    public String parseUrl(String url) throws IOException {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null || !scheme.matches("https?")) {
            throw new IllegalArgumentException("URL inválida: apenas HTTP/HTTPS são permitidos. Esquema: " + scheme);
        }
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("URL inválida: host não encontrado.");
        }
        if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0")
                || host.startsWith("10.") || host.startsWith("172.16.") || host.startsWith("192.168.")
                || host.endsWith(".local") || host.endsWith(".internal")) {
            throw new IllegalArgumentException("URL rejeitada: endereços internos não são permitidos: " + host);
        }

        log.info("Baixando conteúdo da URL: {}", url);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
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

        if (result.isEmpty()) {
            throw new com.project.chat.exception.ValidationException("Não foi possível extrair conteúdo textual da URL fornecida.");
        }

        return result;
    }

    @Override
    public boolean supports(String sourceType) {
        return "url".equalsIgnoreCase(sourceType) || "web".equalsIgnoreCase(sourceType);
    }
}

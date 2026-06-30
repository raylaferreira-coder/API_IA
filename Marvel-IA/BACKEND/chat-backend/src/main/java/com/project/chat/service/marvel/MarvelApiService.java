package com.project.chat.service.marvel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MarvelApiService {

    private static final Logger log = LoggerFactory.getLogger(MarvelApiService.class);

    private final MarvelApiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MarvelApiService(MarvelApiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<MarvelCharacter> fetchCharacters(int offset) {
        String ts = String.valueOf(Instant.now().toEpochMilli());
        String hash = md5(ts + properties.getPrivateKey() + properties.getPublicKey());
        String url = String.format("%s/characters?ts=%s&apikey=%s&hash=%s&limit=%d&offset=%d",
                properties.getBaseUrl(), ts, properties.getPublicKey(), hash, properties.getLimit(), offset);

        return fetchList(url, "characters", MarvelCharacter::fromJson);
    }

    public List<MarvelComic> fetchComics(int offset) {
        String ts = String.valueOf(Instant.now().toEpochMilli());
        String hash = md5(ts + properties.getPrivateKey() + properties.getPublicKey());
        String url = String.format("%s/comics?ts=%s&apikey=%s&hash=%s&limit=%d&offset=%d&orderBy=-modified",
                properties.getBaseUrl(), ts, properties.getPublicKey(), hash, properties.getLimit(), offset);

        return fetchList(url, "comics", MarvelComic::fromJson);
    }

    public List<MarvelSeries> fetchSeries(int offset) {
        String ts = String.valueOf(Instant.now().toEpochMilli());
        String hash = md5(ts + properties.getPrivateKey() + properties.getPublicKey());
        String url = String.format("%s/series?ts=%s&apikey=%s&hash=%s&limit=%d&offset=%d&orderBy=-modified",
                properties.getBaseUrl(), ts, properties.getPublicKey(), hash, properties.getLimit(), offset);

        return fetchList(url, "series", MarvelSeries::fromJson);
    }

    public boolean isConfigured() {
        return properties.getPublicKey() != null && !properties.getPublicKey().isEmpty()
                && properties.getPrivateKey() != null && !properties.getPrivateKey().isEmpty();
    }

    private <T> List<T> fetchList(String url, String dataKey, JsonParser<T> parser) {
        List<T> results = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(properties.getReadTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Marvel API erro {}: {}", response.statusCode(), response.body());
                return results;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            JsonNode items = data.path("results");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    try {
                        results.add(parser.parse(item));
                    } catch (Exception e) {
                        log.warn("Erro ao parsear item Marvel API: {}", e.getMessage());
                    }
                }
            }

            log.info("Marvel API: {} {} retornados (total: {})", results.size(), dataKey, data.path("total").asInt());
        } catch (Exception e) {
            log.error("Erro ao buscar {} da Marvel API: {}", dataKey, e.getMessage());
        }
        return results;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return String.format("%032x", new BigInteger(1, digest));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash MD5", e);
        }
    }

    @FunctionalInterface
    private interface JsonParser<T> {
        T parse(JsonNode node);
    }

    public static class MarvelCharacter {
        private long id;
        private String name;
        private String description;
        private String thumbnail;
        private List<String> comics = new ArrayList<>();
        private List<String> series = new ArrayList<>();

        public long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getThumbnail() { return thumbnail; }
        public List<String> getComics() { return comics; }
        public List<String> getSeries() { return series; }

        static MarvelCharacter fromJson(JsonNode node) {
            MarvelCharacter c = new MarvelCharacter();
            c.id = node.path("id").asLong();
            c.name = node.path("name").asText("");
            c.description = node.path("description").asText("");
            JsonNode thumb = node.path("thumbnail");
            if (!thumb.isMissingNode()) {
                c.thumbnail = thumb.path("path").asText() + "." + thumb.path("extension").asText();
            }
            JsonNode comicsNode = node.path("comics").path("items");
            if (comicsNode.isArray()) {
                comicsNode.forEach(item -> c.comics.add(item.path("name").asText()));
            }
            JsonNode seriesNode = node.path("series").path("items");
            if (seriesNode.isArray()) {
                seriesNode.forEach(item -> c.series.add(item.path("name").asText()));
            }
            return c;
        }
    }

    public static class MarvelComic {
        private long id;
        private String title;
        private String description;
        private String thumbnail;

        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getThumbnail() { return thumbnail; }

        static MarvelComic fromJson(JsonNode node) {
            MarvelComic c = new MarvelComic();
            c.id = node.path("id").asLong();
            c.title = node.path("title").asText("");
            c.description = node.path("description").asText("");
            JsonNode thumb = node.path("thumbnail");
            if (!thumb.isMissingNode()) {
                c.thumbnail = thumb.path("path").asText() + "." + thumb.path("extension").asText();
            }
            return c;
        }
    }

    public static class MarvelSeries {
        private long id;
        private String title;
        private String description;

        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }

        static MarvelSeries fromJson(JsonNode node) {
            MarvelSeries s = new MarvelSeries();
            s.id = node.path("id").asLong();
            s.title = node.path("title").asText("");
            s.description = node.path("description").asText("");
            return s;
        }
    }
}

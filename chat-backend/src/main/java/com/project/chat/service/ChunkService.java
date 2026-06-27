package com.project.chat.service;

import com.project.chat.config.ChunkingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkService {

    private static final Logger log = LoggerFactory.getLogger(ChunkService.class);

    private final int chunkSize;
    private final int overlap;

    public ChunkService(ChunkingProperties properties) {
        this.chunkSize = properties.getSize();
        this.overlap = properties.getOverlap();
        log.info("Chunking config: size={}, overlap={}", chunkSize, overlap);
    }

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        int length = normalized.length();

        if (length <= chunkSize) {
            chunks.add(normalized);
            return chunks;
        }

        int start = 0;
        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            if (end < length) {
                int boundary = findSentenceBoundary(normalized, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            chunks.add(normalized.substring(start, end).trim());
            start = end - overlap;
            if (start >= length) break;
        }

        log.debug("Texto dividido em {} chunks", chunks.size());
        return chunks;
    }

    private int findSentenceBoundary(String text, int fromPos) {
        int searchStart = Math.max(0, fromPos - 100);
        for (int i = fromPos; i > searchStart; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i + 1;
            }
        }
        return fromPos;
    }

    public int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }
}

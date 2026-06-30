package com.project.chat.service;

import com.project.chat.ai.prompt.MarvelPromptBuilder;
import com.project.chat.entity.DocumentChunk;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    private final MarvelPromptBuilder marvelPromptBuilder;

    public PromptBuilder(MarvelPromptBuilder marvelPromptBuilder) {
        this.marvelPromptBuilder = marvelPromptBuilder;
    }

    public String buildWithContext(String question, List<DocumentChunk> chunks) {
        String chunksContext = buildChunkContext(chunks);
        if (chunksContext.isBlank()) {
            return marvelPromptBuilder.buildSimplePrompt(question);
        }
        return marvelPromptBuilder.buildPromptWithContext(question, chunksContext);
    }

    public String buildWithRawContext(String question, String rawContext) {
        if (rawContext == null || rawContext.isBlank()) {
            return marvelPromptBuilder.buildSimplePrompt(question);
        }
        return marvelPromptBuilder.buildFromRawContext(question, rawContext);
    }

    public String buildChunkContext(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        return chunks.stream()
                .map(c -> "- " + c.getContent())
                .collect(Collectors.joining("\n\n"));
    }

}

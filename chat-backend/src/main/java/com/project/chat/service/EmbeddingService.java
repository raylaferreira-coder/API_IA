package com.project.chat.service;

import java.util.List;

public interface EmbeddingService {
    float[] generateEmbedding(String text);
    List<float[]> generateEmbeddings(List<String> texts);
}

package com.project.chat.service;

import java.util.List;

public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> embedAll(List<String> texts);
}

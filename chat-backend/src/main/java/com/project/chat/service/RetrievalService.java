package com.project.chat.service;

import com.project.chat.entity.DocumentChunk;
import java.util.List;

public interface RetrievalService {

    List<DocumentChunk> search(String query, int topK);

    List<DocumentChunk> search(float[] vector, int topK);

}

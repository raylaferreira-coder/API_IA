package com.project.chat.service;

import com.project.chat.entity.DocumentChunk;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Profile("dev")
public class NoopRetrievalService implements RetrievalService {

    @Override
    public List<DocumentChunk> search(String query, int topK) {
        return List.of();
    }

    @Override
    public List<DocumentChunk> search(float[] vector, int topK) {
        return List.of();
    }

}

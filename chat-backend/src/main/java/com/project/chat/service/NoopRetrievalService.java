package com.project.chat.service;

import com.project.chat.entity.DocumentChunk;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NoopRetrievalService implements RetrievalService {

    @Override
    public List<DocumentChunk> search(String query, int topK) {
        return List.of();
    }

}

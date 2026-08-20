package com.skala.helpdesk.rag;

import java.util.List;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RetrievalService {

    private final VectorStore vectorStore;
    private final int defaultTopK;
    private final double similarityThreshold;

    public RetrievalService(
            VectorStore vectorStore,
            @Value("${helpdesk.rag.top-k:4}") int defaultTopK,
            @Value("${helpdesk.rag.similarity-threshold:0.5}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.defaultTopK = defaultTopK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<ContextChunk> retrieve(String question) {
        return retrieve(question, defaultTopK);
    }

    public List<ContextChunk> retrieve(String question, int topK) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("질문은 비어 있을 수 없습니다.");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK는 1 이상 20 이하여야 합니다.");
        }

        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build())
                .stream()
                .map(document -> new ContextChunk(
                        String.valueOf(document.getMetadata().get("source")),
                        document.getScore(),
                        document.getText()))
                .toList();
    }
}

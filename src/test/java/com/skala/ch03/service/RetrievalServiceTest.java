package com.skala.ch03.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.skala.ch03.rag.ContextChunk;
import com.skala.ch03.rag.RetrievalService;

class RetrievalServiceTest {

    @Test
    void returnsSourceScoreAndFullChunk() {
        VectorStore vectorStore = mock(VectorStore.class);
        String fullText = "가".repeat(150);
        Document document = Document.builder()
                .text(fullText)
                .metadata(Map.of("source", "return-policy"))
                .score(0.82)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));
        var service = new RetrievalService(vectorStore, 4, 0.5);

        List<ContextChunk> result = service.retrieve("반품 기한");

        assertThat(result).containsExactly(new ContextChunk("return-policy", 0.82, fullText));
        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTopK()).isEqualTo(4);
        assertThat(requestCaptor.getValue().getSimilarityThreshold()).isEqualTo(0.5);
    }

    @Test
    void rejectsInvalidQuestionAndTopKBeforeEmbeddingCall() {
        VectorStore vectorStore = mock(VectorStore.class);
        var service = new RetrievalService(vectorStore, 4, 0.5);

        assertThatThrownBy(() -> service.retrieve("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.retrieve("질문", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

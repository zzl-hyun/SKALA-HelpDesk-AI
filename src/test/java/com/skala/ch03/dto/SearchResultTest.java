package com.skala.ch03.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.skala.ch03.rag.ContextChunk;

class SearchResultTest {

    @Test
    void limitsDiagnosticContentToOneHundredTwentyCharacters() {
        var chunk = new ContextChunk("source", 0.7, "가".repeat(121));

        SearchResult response = SearchResult.from(chunk);

        assertThat(response.content()).hasSize(123).endsWith("...");
    }
}

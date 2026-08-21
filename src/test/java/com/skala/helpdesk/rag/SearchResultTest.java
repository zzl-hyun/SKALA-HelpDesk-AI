package com.skala.helpdesk.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchResultTest {

    @Test
    void limitsDiagnosticContentToOneHundredTwentyCharacters() {
        var chunk = new ContextChunk("source", 0.7, "가".repeat(121));

        SearchResult response = SearchResult.from(chunk);

        assertThat(response.content()).hasSize(123).endsWith("...");
    }
}

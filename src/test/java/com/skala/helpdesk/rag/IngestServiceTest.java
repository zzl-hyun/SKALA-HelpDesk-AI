package com.skala.helpdesk.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.ByteArrayResource;

class IngestServiceTest {

    @Test
    void replacesSameSourceAndKeepsMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        var service = new IngestService(vectorStore, 400, 200);
        var resource =
                new ByteArrayResource("단순 변심 반품은 7일 이내 가능합니다.".getBytes(StandardCharsets.UTF_8));

        var result = service.ingest(resource, "return-policy", "1.0");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        InOrder order = inOrder(vectorStore);
        order.verify(vectorStore).delete(any(Filter.Expression.class));
        order.verify(vectorStore).add(chunksCaptor.capture());

        assertThat(result.source()).isEqualTo("return-policy");
        assertThat(result.chunks()).isEqualTo(1);
        assertThat(chunksCaptor.getValue())
                .singleElement()
                .satisfies(
                        chunk -> {
                            assertThat(chunk.getMetadata())
                                    .containsEntry("source", "return-policy");
                            assertThat(chunk.getMetadata()).containsEntry("version", "1.0");
                        });
    }
}

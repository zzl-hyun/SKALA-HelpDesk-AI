package com.skala.helpdesk.rag;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class IngestService {

    private static final String DOCUMENT_VERSION = "1.0";

    private static final List<SourceDocument> DEFAULT_DOCUMENTS =
            List.of(
                    new SourceDocument("return-policy", "knowledge/return-policy.md"),
                    new SourceDocument("shipping-policy", "knowledge/shipping-policy.md"),
                    new SourceDocument("membership", "knowledge/membership.md"));

    private final VectorStore vectorStore;
    private final int chunkSize;
    private final int minChunkSizeChars;

    public IngestService(
            VectorStore vectorStore,
            @Value("${helpdesk.rag.chunk-size:400}") int chunkSize,
            @Value("${helpdesk.rag.min-chunk-size-chars:200}") int minChunkSizeChars) {
        this.vectorStore = vectorStore;
        this.chunkSize = chunkSize;
        this.minChunkSizeChars = minChunkSizeChars;
    }

    public List<IngestResult> ingestDefaultDocuments() {
        return DEFAULT_DOCUMENTS.stream()
                .map(
                        document ->
                                ingest(
                                        new ClassPathResource(document.path()),
                                        document.source(),
                                        DOCUMENT_VERSION))
                .toList();
    }

    public IngestResult ingest(Resource doc, String source, String version) {

        var reader = new TextReader(doc);
        List<Document> documents = reader.get();

        documents.forEach(
                document -> {
                    document.getMetadata().put("source", source);
                    document.getMetadata().put("version", version);
                });

        var splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(chunkSize)
                        .withMinChunkSizeChars(minChunkSizeChars)
                        .build();

        List<Document> chunks = splitter.apply(documents);

        vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());

        vectorStore.add(chunks);

        return new IngestResult(source, chunks.size());
    }

    public record IngestResult(String source, int chunks) {}

    private record SourceDocument(String source, String path) {}
}

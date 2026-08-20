package com.skala.ch03.rag;

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
public class DocumentService {

        private static final String DOCUMENT_VERSION = "1.0";

        private static final List<SourceDocument> DEFAULT_DOCUMENTS = List.of(
                        new SourceDocument("return-policy", "knowledge/return-policy.md"),
                        new SourceDocument("shipping-policy", "knowledge/shipping-policy.md"),
                        new SourceDocument("membership", "knowledge/membership.md"));

        private final VectorStore vectorStore;
        private final int chunkSize;
        private final int minChunkSizeChars;

        public DocumentService(
                        VectorStore vectorStore,
                        @Value("${lab2.rag.chunk-size:400}") int chunkSize,
                        @Value("${lab2.rag.min-chunk-size-chars:200}") int minChunkSizeChars) {
                this.vectorStore = vectorStore;
                this.chunkSize = chunkSize;
                this.minChunkSizeChars = minChunkSizeChars;
        }

        /**
         * 실습용 정책 문서 세 개를 한 번에 다시 색인한다.
         *
         * <p>
         * 각 문서는 {@link #ingest(Resource, String, String)} 안에서 같은 source의
         * 기존 청크를 먼저 지우므로, 이 API를 반복 호출해도 중복이 쌓이지 않는다.
         */
        public List<IngestResult> ingestDefaultDocuments() {
                return DEFAULT_DOCUMENTS.stream()
                                .map(document -> ingest(
                                                new ClassPathResource(document.path()),
                                                document.source(),
                                                DOCUMENT_VERSION))
                                .toList();
        }

        public IngestResult ingest(Resource doc, String source, String version) {

                var reader = new TextReader(doc);
                List<Document> documents = reader.get();

                // TextReader#get()이 source를 파일명으로 덮어쓰므로 읽은 다음 고정한다.
                documents.forEach(document -> {
                        document.getMetadata().put("source", source);
                        document.getMetadata().put("version", version);
                });

                var splitter = TokenTextSplitter.builder()
                                .withChunkSize(chunkSize)
                                .withMinChunkSizeChars(minChunkSizeChars)
                                .build();

                List<Document> chunks = splitter.apply(documents);

                vectorStore.delete(new FilterExpressionBuilder()
                                .eq("source", source)
                                .build());

                vectorStore.add(chunks);

                return new IngestResult(source, chunks.size());
        }

        public record IngestResult(String source, int chunks) {
        }

        private record SourceDocument(String source, String path) {
        }
}

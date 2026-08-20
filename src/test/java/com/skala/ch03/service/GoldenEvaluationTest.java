package com.skala.ch03.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.ch03.dto.ChatResponse;
import com.skala.ch03.rag.DocumentService;

@Tag("eval")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoldenEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(GoldenEvaluationTest.class);

    @Autowired
    private DocumentService ingestService;

    @Autowired
    private AssistantService questionAnswerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${lab2.rag.chunk-size}")
    private int chunkSize;

    @Value("${lab2.rag.top-k}")
    private int topK;

    @Value("${lab2.rag.similarity-threshold}")
    private double similarityThreshold;

    @BeforeAll
    void ingestDocuments() {
        log.info("실험 설정: chunkSize={}, topK={}, threshold={}",
                chunkSize, topK, similarityThreshold);
        var results = ingestService.ingestDefaultDocuments();
        log.info("인제스트 결과: {}", results);
    }

    @Test
    void goldenSetPassesAtLeastEightQuestions() throws IOException {
        List<Golden> golden = readGoldenSet();
        int pass = 0;

        for (Golden expected : golden) {
            ChatResponse actual = questionAnswerService.ask(expected.q());

            boolean hasRequiredWords = expected.must().stream()
                    .allMatch(keyword -> actual.answer().contains(keyword));
            boolean hasExpectedSource = expected.src() == null
                    ? !actual.grounded() && actual.sources().isEmpty()
                    : actual.sources().stream().anyMatch(source -> source.contains(expected.src()));

            if (hasRequiredWords && hasExpectedSource) {
                pass++;
            } else {
                log.warn("실패: {}\n  답변: {}\n  출처: {}\n  grounded: {}",
                        expected.q(), actual.answer(), actual.sources(), actual.grounded());
            }
        }

        log.info("골든 세트 통과 {}/{}", pass, golden.size());
        assertThat(pass).isGreaterThanOrEqualTo(8);
    }

    private List<Golden> readGoldenSet() throws IOException {
        var resource = new ClassPathResource("knowledge/golden.json");
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        }
    }

    private record Golden(String q, List<String> must, String src) {
    }
}

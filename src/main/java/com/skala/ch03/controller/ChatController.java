package com.skala.ch03.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skala.ch03.dto.AskRequest;
import com.skala.ch03.dto.ChatResponse;
import com.skala.ch03.dto.SearchResult;
import com.skala.ch03.rag.DocumentService;
import com.skala.ch03.rag.DocumentService.IngestResult;
import com.skala.ch03.rag.RetrievalService;
import com.skala.ch03.service.AssistantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lab2")
@Tag(name = "Day2 실습 · 사내 문서 Q&A")
public class ChatController {

    private final DocumentService ingestService;
    private final RetrievalService retrievalService;
    private final AssistantService questionAnswerService;

    public ChatController(
            DocumentService ingestService,
            RetrievalService retrievalService,
            AssistantService questionAnswerService) {
        this.ingestService = ingestService;
        this.retrievalService = retrievalService;
        this.questionAnswerService = questionAnswerService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "정책 문서 다시 색인", description = "같은 source의 기존 청크를 지운 뒤 세 문서를 저장합니다.")
    public List<IngestResult> ingest() {
        log.info("Starting document ingestion for lab2 default documents");
        var result = ingestService.ingestDefaultDocuments();
        log.info("Document ingestion completed. processedCount={}", result.size());
        return result;
    }

    @GetMapping("/retrieve")
    @Operation(summary = "검색 결과와 유사도 확인", description = "답변 생성 전에 검색 품질을 점수와 함께 확인합니다.")
    public List<SearchResult> retrieve(
            @RequestParam String q,
            @RequestParam(required = false) Integer topK) {
        log.info("Retrieval request received. query={}, topK={}", q, topK);
        try {
            var chunks = topK == null
                    ? retrievalService.retrieve(q)
                    : retrievalService.retrieve(q, topK);
            var responses = chunks.stream().map(SearchResult::from).toList();
            log.info("Retrieval succeeded. query={}, returnedChunks={}", q, responses.size());
            return responses;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid retrieval request. query={}, topK={}", q, topK, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/ask")
    @Operation(summary = "정책 문서에 근거해 답변", description = "답변, 사용한 출처, 근거 사용 여부를 구조화해 반환합니다.")
    public ChatResponse ask(@RequestBody AskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            log.warn("Empty ask request received");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문은 비어 있을 수 없습니다.");
        }
        if (request == null || request.userId() == null || request.userId().isBlank()) {
            log.warn("Empty ask request received");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId는 비어 있을 수 없습니다.");
        }

        log.info("Question asked: {}", request.question());
        var answer = questionAnswerService.ask(request.question(), request.userId());
        log.info("Question answered successfully: {}", request.question());
        return answer;
    }

}

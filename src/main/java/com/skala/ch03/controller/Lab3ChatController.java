package com.skala.ch03.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skala.ch03.dto.AskRequest;
import com.skala.ch03.dto.Lab3ChatResponse;
import com.skala.ch03.service.Lab3ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lab3")
@Tag(name = "Day3 실습 · 상담 에이전트")
public class Lab3ChatController {

    private final Lab3ChatService lab3ChatService;

    public Lab3ChatController(Lab3ChatService lab3ChatService) {
        this.lab3ChatService = lab3ChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "상담 에이전트 대화", description = "정책 문서 근거(RAG)와 실시간 주문 조회(Tool)를 함께 처리합니다.")
    public Lab3ChatResponse chat(@RequestBody AskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문은 비어 있을 수 없습니다.");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId는 비어 있을 수 없습니다.");
        }

        log.info("Lab3 chat requested. userId={}, question={}", request.userId(), request.question());
        String answer = lab3ChatService.chat(request.question(), request.userId());
        log.info("Lab3 chat answered. userId={}", request.userId());
        return new Lab3ChatResponse(answer);
    }
}

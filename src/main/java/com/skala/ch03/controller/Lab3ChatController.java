package com.skala.ch03.controller;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skala.ch03.dto.AskRequest;
import com.skala.ch03.dto.Lab3ChatResponse;
import com.skala.ch03.dto.MessageView;
import com.skala.ch03.service.Lab3ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lab3")
@Tag(name = "Day3 실습 · 상담 에이전트")
public class Lab3ChatController {

    private final Lab3ChatService lab3ChatService;
    private final ChatMemory chatMemory;

    public Lab3ChatController(Lab3ChatService lab3ChatService, ChatMemory chatMemory) {
        this.lab3ChatService = lab3ChatService;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "상담 에이전트 대화",
            description = "정책 문서 근거(RAG)와 실시간 주문 조회(Tool)를 함께 처리합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AskRequest.class),
                            examples = @ExampleObject(
                                    name = "주문 조회 예시",
                                    value = """
                                            {
                                              "userId": "user-1",
                                              "sessionId": "s1",
                                              "question": "제 주문 12345는 지금 어디예요?"
                                            }
                                            """))))
    public Lab3ChatResponse chat(@RequestBody AskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문은 비어 있을 수 없습니다.");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId는 비어 있을 수 없습니다.");
        }

        log.info("Lab3 chat requested. userId={}, sessionId={}, question={}",
                request.userId(), request.sessionId(), request.question());
        Lab3ChatResponse response = lab3ChatService.chat(request.question(), request.userId(), request.sessionId());
        log.info("Lab3 chat answered. userId={}", request.userId());
        return response;
    }

    @GetMapping("/chat/history")
    @Operation(summary = "대화 이력 조회", description = "세션 단위로 저장된 대화 이력을 확인한다. sessionId를 생략하면 default 세션을 본다.")
    public List<MessageView> history(
            @RequestParam String userId,
            @RequestParam(required = false) String sessionId) {
        String conversationId = Lab3ChatService.conversationId(userId, sessionId);
        return chatMemory.get(conversationId).stream()
                .map(message -> new MessageView(message.getMessageType().name(), message.getText()))
                .toList();
    }
}

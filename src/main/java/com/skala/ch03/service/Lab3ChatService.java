package com.skala.ch03.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.skala.ch03.dto.Lab3ChatResponse;
import com.skala.ch03.handler.exception.UnsafeInputException;

@Service
public class Lab3ChatService {

    private final ChatClient lab3ChatClient;

    public Lab3ChatService(@Qualifier("lab3ChatClient") ChatClient lab3ChatClient) {
        this.lab3ChatClient = lab3ChatClient;
    }

    /**
     * 대화 ID 규칙을 만드는 곳은 여기 한 곳뿐이어야 한다 — 흩어지면 남의 대화가 섞이는 사고가 난다.
     * Controller의 history() 조회도 반드시 이 메서드를 통해서만 conversationId를 만들어야 한다.
     */
    public static String conversationId(String userId, String sessionId) {
        String session = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
        return "%s:%s".formatted(userId, session);
    }

    /** MDC 키 — advisor들이 같은 요청의 로그를 같은 값으로 묶어 찍을 때 이 키를 읽는다. */
    public static final String TRACE_ID = "traceId";

    public Lab3ChatResponse chat(String question, String userId, String sessionId) {
        String conversationId = conversationId(userId, sessionId);
        MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
        try {
            ChatClientResponse response = lab3ChatClient.prompt()
                    .user(question)
                    .advisors(a -> a
                            .param(ChatMemory.CONVERSATION_ID, conversationId)
                            .param("userId", userId)) // AuditAdvisor 등 advisor 체인에서 쓰는 값 — toolContext와는 별도 통로다.
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .chatClientResponse();

            String answer = response.chatResponse().getResult().getOutput().getText();
            List<String> sources = extractSources(response);
            return new Lab3ChatResponse(answer, sources);
        } catch (UnsafeInputException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 호출에 실패했습니다.", e);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    /** RAG가 실제로 근거로 쓴 문서 출처를 응답 컨텍스트에서 꺼낸다 — Advisor는 근거를 넣어줄 뿐, 출처 표기는 우리 몫이다. */
    @SuppressWarnings("unchecked")
    private static List<String> extractSources(ChatClientResponse response) {
        Object retrieved = response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (!(retrieved instanceof List<?> documents)) {
            return List.of();
        }
        return ((List<Document>) documents).stream()
                .map(document -> String.valueOf(document.getMetadata().get("source")))
                .distinct()
                .toList();
    }
}

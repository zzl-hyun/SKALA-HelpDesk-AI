package com.skala.helpdesk.service;

import com.skala.helpdesk.api.chat.response.AnswerDto;
import com.skala.helpdesk.handler.exception.UnsafeInputException;
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
import org.springframework.stereotype.Service;

@Service
public class HelpDeskService {

    private static final String FALLBACK_ANSWER =
            "현재 상담 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해 주세요." + " 긴급한 문의는 담당자에게 직접 접수해 주세요.";

    private final ChatClient helpDeskChatClient;

    public HelpDeskService(@Qualifier("helpDeskChatClient") ChatClient helpDeskChatClient) {
        this.helpDeskChatClient = helpDeskChatClient;
    }

    /** 대화 ID 규칙은 서비스에서만 관리한다. */
    public static String conversationId(String userId, String sessionId) {
        String session = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
        return "%s:%s".formatted(userId, session);
    }

    /** advisor들이 같은 요청을 묶어 로그를 남길 때 사용하는 MDC 키. */
    public static final String TRACE_ID = "traceId";

    public AnswerDto chat(String question, String userId, String sessionId) {
        String conversationId = conversationId(userId, sessionId);
        MDC.put(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
        try {
            ChatClientResponse response =
                    helpDeskChatClient
                            .prompt()
                            .user(question)
                            .advisors(
                                    a ->
                                            a.param(ChatMemory.CONVERSATION_ID, conversationId)
                                                    .param("userId", userId))
                            .toolContext(Map.of("userId", userId))
                            .call()
                            .chatClientResponse();

            String answer = response.chatResponse().getResult().getOutput().getText();
            List<String> sources = extractSources(response);
            return new AnswerDto(answer, sources);
        } catch (UnsafeInputException e) {
            throw e;
        } catch (RuntimeException e) {
            return new AnswerDto(FALLBACK_ANSWER, List.of());
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractSources(ChatClientResponse response) {
        Object retrieved = response.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (!(retrieved instanceof List<?> documents)) {
            return List.of();
        }
        return ((List<Document>) documents)
                .stream()
                        .map(document -> String.valueOf(document.getMetadata().get("source")))
                        .distinct()
                        .toList();
    }
}

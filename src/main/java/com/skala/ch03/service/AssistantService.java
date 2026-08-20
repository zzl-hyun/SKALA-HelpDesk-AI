package com.skala.ch03.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.skala.ch03.dto.ChatResponse;
import com.skala.ch03.rag.ContextChunk;
import com.skala.ch03.rag.RetrievalService;
import com.skala.ch03.tool.OrderTool;

@Service
public class AssistantService {

    private static final String UNKNOWN_PHRASE = "확인되지";

    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final OrderTool orderTool;

    public AssistantService(
            RetrievalService retrievalService,
            @Qualifier("extractClient") ChatClient chatClient,
            OrderTool orderTool) {
        this.retrievalService = retrievalService;
        this.chatClient = chatClient;
        this.orderTool = orderTool;
    }

    /**
     *
     * @param question
     * @return
     */
    public ChatResponse ask(String question, String userId) {
        final List<ContextChunk> chunks;
        try {
            chunks = retrievalService.retrieve(question);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

        // 검색된 근거가 하나도 없으면 생성 모델을 호출하지 않는다.
        if (chunks.isEmpty()) {
            return ChatResponse.unknown();
        }

        final ChatResponse generated;
        try {
            generated = chatClient.prompt()
                    .system("""
                            아래 [근거]만 사용해 답한다.
                            근거에 없으면 "확인되지 않습니다."라고 답하고 grounded를 false로 둔다.
                            추측하지 않는다.
                            sources에는 실제로 사용한 근거의 source 값을 그대로 넣는다.
                            답변은 간결한 한국어 존댓말로 작성한다.
                            """)
                    .user(user -> user.text("""
                            [근거]
                            {context}

                            [질문]
                            {question}
                            """)
                            .param("context", format(chunks))
                            .param("question", question))
                    .tools(orderTool)
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .entity(ChatResponse.class);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 호출에 실패했습니다.", e);
        }

        return validateGrounding(generated, chunks);
    }

    private static String format(List<ContextChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (ContextChunk chunk : chunks) {
            context.append("[source=")
                    .append(chunk.source())
                    .append("]\n")
                    .append(chunk.content())
                    .append("\n\n");
        }
        return context.toString().trim();
    }

    /** 모델이 검색되지 않은 출처를 만들어 내면 응답 단계에서 제거한다. */
    static ChatResponse validateGrounding(ChatResponse generated, List<ContextChunk> chunks) {
        if (generated == null || generated.answer() == null || generated.answer().isBlank()) {
            return ChatResponse.unknown();
        }

        boolean unknown = generated.answer().contains(UNKNOWN_PHRASE);
        if (unknown || !generated.grounded()) {
            return ChatResponse.unknown();
        }

        Set<String> availableSources = new LinkedHashSet<>();
        chunks.stream().map(ContextChunk::source).forEach(availableSources::add);

        List<String> verifiedSources = generated.sources() == null
                ? List.of()
                : generated.sources().stream()
                        .map(source -> canonicalSource(source, availableSources))
                        .filter(source -> source != null)
                        .distinct()
                        .toList();

        // 출처 없는 답을 근거 있는 답으로 내보내지 않는다.
        if (verifiedSources.isEmpty()) {
            return ChatResponse.unknown();
        }

        return new ChatResponse(generated.answer(), verifiedSources, true);
    }

    private static String canonicalSource(String candidate, Set<String> availableSources) {
        if (candidate == null) {
            return null;
        }

        String normalized = candidate.toLowerCase(Locale.ROOT).replace(".md", "").trim();
        return availableSources.stream()
                .filter(source -> normalized.equals(source.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }
}

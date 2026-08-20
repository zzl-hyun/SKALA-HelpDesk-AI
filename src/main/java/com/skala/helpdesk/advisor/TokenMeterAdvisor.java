package com.skala.helpdesk.advisor;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Component;

import com.skala.helpdesk.chat.HelpDeskService;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * order 900 — 가장 안쪽. 실제로 모델까지 다녀온 뒤의 진짜 토큰·지연을 재야 하니까 맨 마지막에 둔다.
 * before/after가 같은 스레드에서 순서대로 실행되는 동기 호출(.call())을 전제로 ThreadLocal로 시작 시각을 넘긴다.
 * 스트리밍(.stream())은 이 방식으로 못 잰다 — 별도 StreamAdvisor가 필요하다.
 */
@Component
public class TokenMeterAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger("METRICS");

    private final MeterRegistry registry;
    private final ThreadLocal<Long> startedAt = new ThreadLocal<>();

    public TokenMeterAdvisor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        startedAt.set(System.nanoTime());
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        try {
            Long start = startedAt.get();
            double elapsedSeconds = 0;
            if (start != null) {
                long elapsedNanos = System.nanoTime() - start;
                elapsedSeconds = elapsedNanos / 1_000_000_000.0;
                registry.timer("ai.latency", "phase", "call").record(elapsedNanos, TimeUnit.NANOSECONDS);
            }

            Integer promptTokens = null;
            Integer completionTokens = null;
            if (response.chatResponse() != null) {
                Usage usage = response.chatResponse().getMetadata().getUsage();
                if (usage != null) {
                    promptTokens = usage.getPromptTokens();
                    completionTokens = usage.getCompletionTokens();
                    if (promptTokens != null) {
                        registry.counter("ai.tokens", "type", "prompt").increment(promptTokens);
                    }
                    if (completionTokens != null) {
                        registry.counter("ai.tokens", "type", "completion").increment(completionTokens);
                    }
                }
            }

            log.info("[{}] 응답 {}초 · 프롬프트 {} · 완성 {} 토큰",
                    MDC.get(HelpDeskService.TRACE_ID), String.format("%.1f", elapsedSeconds), promptTokens, completionTokens);
            return response;
        } finally {
            startedAt.remove();
        }
    }

    @Override
    public int getOrder() {
        return 900;
    }
}

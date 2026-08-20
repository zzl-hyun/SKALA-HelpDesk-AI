package com.skala.ch03.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

import com.skala.ch03.service.Lab3ChatService;

/**
 * 가장 바깥(order 0)에 둔다 — 다른 advisor가 뭘 하든 상관없이 요청·응답을 있는 그대로 기록해야 하니까.
 * 감사 로그는 SafetyAdvisor가 막은 요청도 남아야 한다.
 */
@Component
public class AuditAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String userId = String.valueOf(request.context().get("userId"));
        String question = request.prompt().getUserMessage() != null
                ? request.prompt().getUserMessage().getText()
                : "";
        log.info("[{}] REQUEST userId={} question={}", MDC.get(Lab3ChatService.TRACE_ID), userId, question);
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        String answer = response.chatResponse() != null
                ? response.chatResponse().getResult().getOutput().getText()
                : "";
        log.info("[{}] RESPONSE answer={}", MDC.get(Lab3ChatService.TRACE_ID), answer);
        return response;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

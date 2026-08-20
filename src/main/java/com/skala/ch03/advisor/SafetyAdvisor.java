package com.skala.ch03.advisor;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

import com.skala.ch03.handler.exception.UnsafeInputException;

/**
 * order 100 — 메모리(200)에 저장되기 전에 차단해야 한다.
 * 여기서 걸러진 입력은 대화 이력에 남지 않는다. 차단은 코드로 강제한다(프롬프트로 "하지 마"라고 부탁하지 않는다).
 */
@Component
public class SafetyAdvisor implements BaseAdvisor {

    private static final List<String> INJECTION_PATTERNS = List.of(
            "이전 지시", "시스템 프롬프트", "무시하고", "너는 이제부터");

    private static final List<String> SENSITIVE_WORDS = List.of(
            "주민등록번호", "카드번호");

    private static final int MAX_LENGTH = 2000;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String text = request.prompt().getUserMessage() != null
                ? request.prompt().getUserMessage().getText()
                : "";

        if (text.length() > MAX_LENGTH) {
            throw new UnsafeInputException("입력이 너무 깁니다.");
        }
        for (String pattern : INJECTION_PATTERNS) {
            if (text.contains(pattern)) {
                throw new UnsafeInputException("허용되지 않는 요청입니다.");
            }
        }
        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                throw new UnsafeInputException("민감 정보가 포함된 요청은 처리할 수 없습니다.");
            }
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public int getOrder() {
        return 100;
    }
}

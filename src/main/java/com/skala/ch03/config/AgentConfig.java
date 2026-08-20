package com.skala.ch03.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.skala.ch03.tool.OrderTool;

/**
 * 2장 — ChatClient 빈을 용도별로 나눠 만든다.
 *
 * <p>
 * 하나의 ChatClient 로 모든 일을 시키면 기본값이 서로 충돌한다.
 * 추출은 흔들리면 안 되고(temperature 0), 상담은 자연스러워야 한다(0.7).
 * 용도별 빈으로 나누면 호출부가 옵션을 매번 덮어쓸 필요가 없다.
 */
@Configuration
public class AgentConfig {

        /** 분류·추출 — 같은 입력이면 같은 출력이어야 하는 일. */
        @Bean
        public ChatClient extractClient(ChatClient.Builder builder) {
                return builder
                                .defaultSystem("""
                                                너는 정확한 정보 추출기다.
                                                - 주어진 텍스트에 없는 내용은 절대 만들어 내지 않는다.
                                                - 값을 찾을 수 없으면 null 을 쓴다.
                                                - 요청받은 형식 외의 설명은 덧붙이지 않는다.""")
                                .defaultOptions(ChatOptions.builder()
                                                .temperature(0.0)
                                                .maxTokens(1024)
                                                .build())
                                .build();
        }

        /** 상담·작문 — 자연스러움이 중요한 일. */
        @Bean
        public ChatClient supportClient(ChatClient.Builder builder) {
                return builder
                                .defaultSystem("""
                                                너는 친절하고 간결한 고객 상담원이다.
                                                - 존댓말을 쓰고 3문장 이내로 답한다.
                                                - 확실하지 않으면 모른다고 말하고 담당자 연결을 안내한다.""")
                                .defaultOptions(ChatOptions.builder()
                                                .temperature(0.7)
                                                .build())
                                .defaultAdvisors(new SimpleLoggerAdvisor())
                                .build();
        }

        /**
         * 3장 — 상담 에이전트. RAG는 Advisor로 붙여서 "근거가 있으면 참고"하게 하고,
         * 없으면 (Day2 AssistantService처럼 아예 막지 않고) 모델이 스스로 도구를 부르거나
         * 모른다고 답하도록 판단을 맡긴다.
         */
        @Bean
        public ChatClient lab3ChatClient(ChatClient.Builder builder, VectorStore vectorStore, OrderTool orderTool) {
                return builder
                                .defaultSystem("""
                                                너는 사내 상담 에이전트다.
                                                - 반품·배송 같은 정책 질문은 검색된 [문서] 근거로만 답하고, 근거가 없으면 모른다고 말한다.
                                                - 주문 상태처럼 실시간 정보가 필요한 질문은 반드시 도구를 호출해서 답한다.
                                                - 질문이 애매하면(예: 주문번호가 없으면) 되묻는다.
                                                - 존댓말을 쓰고 간결하게 답한다.""")
                                .defaultAdvisors(
                                                QuestionAnswerAdvisor.builder(vectorStore).build(),
                                                new SimpleLoggerAdvisor())
                                .defaultTools(orderTool)
                                .build();
        }
}

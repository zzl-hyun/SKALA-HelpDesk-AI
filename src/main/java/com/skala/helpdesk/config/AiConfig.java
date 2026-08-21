package com.skala.helpdesk.config;

import com.skala.helpdesk.advisor.AuditAdvisor;
import com.skala.helpdesk.advisor.SafetyAdvisor;
import com.skala.helpdesk.advisor.TokenMeterAdvisor;
import com.skala.helpdesk.tools.OrderTools;
import com.skala.helpdesk.tools.TicketTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 2장 — ChatClient 빈을 용도별로 나눠 만든다.
 *
 * <p>하나의 ChatClient 로 모든 일을 시키면 기본값이 서로 충돌한다. 추출은 흔들리면 안 되고(temperature 0), 상담은 자연스러워야 한다(0.7). 용도별
 * 빈으로 나누면 호출부가 옵션을 매번 덮어쓸 필요가 없다.
 */
@Configuration
public class AiConfig {

//     분류 및 추출
    @Bean
    public ChatClient extractClient(ChatClient.Builder builder) {
        return builder.defaultSystem(
                        """
                                                너는 정확한 정보 추출기다.
                                                - 주어진 텍스트에 없는 내용은 절대 만들어 내지 않는다.
                                                - 값을 찾을 수 없으면 null 을 쓴다.
                                                - 요청받은 형식 외의 설명은 덧붙이지 않는다.""")
                .defaultOptions(ChatOptions.builder().temperature(0.0).maxTokens(1024).build())
                .build();
    }

//     챗봇
    @Bean
    public ChatClient supportClient(ChatClient.Builder builder) {
        return builder.defaultSystem(
                        """
                                                너는 친절하고 간결한 고객 상담원이다.
                                                - 존댓말을 쓰고 3문장 이내로 답한다.
                                                - 확실하지 않으면 모른다고 말하고 담당자 연결을 안내한다.""")
                .defaultOptions(ChatOptions.builder().temperature(0.7).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

//     대화 컨텍스트
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${helpdesk.memory.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

//     Advisor
    @Bean
    public ChatClient helpDeskChatClient(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            ChatMemory chatMemory,
            OrderTools orderTools,
            TicketTools ticketTools,
            AuditAdvisor auditAdvisor,
            SafetyAdvisor safetyAdvisor,
            TokenMeterAdvisor tokenMeterAdvisor,
            @Value("${helpdesk.rag.similarity-threshold:0.33}") double ragThreshold,
            @Value("${helpdesk.rag.top-k:4}") int ragTopK) {
        return builder.defaultSystem(
                        """
                                                너는 사내 상담 에이전트다.
                                                - 반품배송 같은 정책 질문은 검색된 [문서] 근거로만 답하고, 근거가 없으면 모른다고 말한다.
                                                - 주문 상태처럼 실시간 정보가 필요한 질문은 반드시 도구를 호출해서 답한다.
                                                - 사용자가 환불교환을 요청하면, 정책 문서에 불가하다는 내용이 있어도 네가 직접 거절하지 않는다.
                                                  가능/불가 판단은 담당자의 몫이다 — requestRefund 도구를 호출해 접수만 하고,
                                                  "접수되었고 담당자 승인 후 처리된다"고 안내한다. 즉시 처리된 것처럼 말하지 않는다.
                                                - 주문번호나 사유를 이미 이전 대화에서 말했다면 다시 묻지 않고 그 값을 그대로 사용해 도구를 호출한다.
                                                  정말로 대화 어디에도 없을 때만 되묻는다.
                                                - 질문이 애매하면(예: 주문번호가 없으면) 되묻는다.
                                                - 존댓말을 쓰고 간결하게 답한다.""")
                .defaultAdvisors(
                        auditAdvisor, // order 0   가장 바깥
                        safetyAdvisor, // order 100 차단
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(200)
                                .build(), // order 200 기억
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .order(300) // 근거 검색
                                .searchRequest(
                                        SearchRequest.builder()
                                                .topK(ragTopK)
                                                .similarityThreshold(ragThreshold)
                                                .build())
                                .build(),
                        tokenMeterAdvisor, // order 900 계측
                        new SimpleLoggerAdvisor())
                .defaultTools(orderTools, ticketTools)
                .build();
    }
}

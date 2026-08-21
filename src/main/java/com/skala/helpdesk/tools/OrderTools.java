package com.skala.helpdesk.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.helpdesk.domain.Order;
import com.skala.helpdesk.handler.exception.OrderNotFoundException;
import com.skala.helpdesk.repository.OrderRepository;
import com.skala.helpdesk.chat.HelpDeskService;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger("METRICS");

    private final OrderRepository orderRepository;
    private final MeterRegistry registry;

    public OrderTools(OrderRepository orderRepository, MeterRegistry registry) {
        this.orderRepository = orderRepository;
        this.registry = registry;
    }

    @Tool(description = """
            주문 상태를 조회한다. 사용자가 주문번호를 말하거나 '내 주문', '배송 언제' 처럼 물으면 이 도구를 쓴다.
            """)
    public Order getOrder(
            @ToolParam(description = "주문 ID") String orderId,
            ToolContext ctx) {

        String userId = (String) ctx.getContext().get("userId");
        long start = System.nanoTime();
        try {
            Order order = orderRepository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            registry.counter("ai.tool.calls", "tool", "getOrder", "result", "ok").increment();
            logCall(orderId, System.nanoTime() - start);
            return order;
        } catch (RuntimeException e) {
            registry.counter("ai.tool.calls", "tool", "getOrder", "result", "fail").increment();
            logCall(orderId, System.nanoTime() - start);
            throw e;
        }
    }

    private static void logCall(String orderId, long elapsedNanos) {
        log.info("[{}]   도구 getOrder({}) {}ms",
                MDC.get(HelpDeskService.TRACE_ID), orderId, elapsedNanos / 1_000_000);
    }
}

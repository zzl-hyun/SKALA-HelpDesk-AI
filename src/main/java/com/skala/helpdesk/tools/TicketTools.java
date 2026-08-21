package com.skala.helpdesk.tools;

import com.skala.helpdesk.chat.HelpDeskService;
import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.handler.exception.OrderNotFoundException;
import com.skala.helpdesk.repository.OrderRepository;
import com.skala.helpdesk.repository.TicketRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TicketTools {

    private static final Logger log = LoggerFactory.getLogger("METRICS");

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final MeterRegistry registry;

    public TicketTools(
            OrderRepository orderRepository,
            TicketRepository ticketRepository,
            MeterRegistry registry) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.registry = registry;
    }

    @Tool(
            description =
                    """
            환불을 접수한다. 사용자가 환불·반품·교환을 요청하면 이 도구를 호출한다.
            즉시 처리되지 않고 담당자 승인 후 처리된다 — 가능/불가 여부를 네가 미리 판단해서 거절하지 않는다.
            """)
    public String requestRefund(
            @ToolParam(description = "주문번호. 대화에서 이미 조회했던 주문번호가 있으면 그걸 그대로 쓴다. 예: 12345")
                    String orderId,
            @ToolParam(
                            description =
                                    "환불 사유. 사용자가 말한 표현을 그대로 옮긴다. 예: '단순 변심', '사이즈가 안 맞음'. "
                                            + "사용자가 사유를 전혀 언급하지 않았을 때만 되물어라.")
                    String reason,
            ToolContext ctx) {

        String userId = (String) ctx.getContext().get("userId");
        long start = System.nanoTime();
        try {
            orderRepository
                    .findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            Ticket ticket = ticketRepository.create(orderId, userId, reason);
            registry.counter("ai.tool.calls", "tool", "requestRefund", "result", "ok").increment();
            log.info(
                    "[{}]   도구 requestRefund({}) {}ms",
                    MDC.get(HelpDeskService.TRACE_ID),
                    orderId,
                    (System.nanoTime() - start) / 1_000_000);
            return "환불이 접수되었습니다. 티켓 번호 T-%d, 담당자 승인 후 처리됩니다.".formatted(ticket.getId());
        } catch (RuntimeException e) {
            registry.counter("ai.tool.calls", "tool", "requestRefund", "result", "fail")
                    .increment();
            log.info(
                    "[{}]   도구 requestRefund({}) {}ms (실패)",
                    MDC.get(HelpDeskService.TRACE_ID),
                    orderId,
                    (System.nanoTime() - start) / 1_000_000);
            throw e;
        }
    }
}

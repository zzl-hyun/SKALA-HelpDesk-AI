package com.skala.ch03.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.ch03.domain.Order;
import com.skala.ch03.handler.exception.OrderNotFoundException;
import com.skala.ch03.repository.OrderRepository;

@Component
public class OrderTool {
    private final OrderRepository orderRepository;

    public OrderTool(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Tool(description = "주문 상태를 확인하는 도구")
    public Order getOrder(
            @ToolParam(description = "주문 ID") String orderId,
            ToolContext ctx) {

        String userId = (String) ctx.getContext().get("userId");
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

}

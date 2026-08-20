package com.skala.ch03.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.skala.ch03.domain.Order;
import com.skala.ch03.domain.OrderStatus;

/** 실습용 — 실제 DB 대신 메모리에 주문 몇 건만 씨딩해 둔다. */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = Map.of(
            "12345", new Order("12345", "user-1", "무선 이어폰", OrderStatus.SHIPPING, "2026-08-20"),

            "99999", new Order("99999", "user-2", "노트북 거치대", OrderStatus.DELIVERED, "2026-08-15"));

    @Override
    public Optional<Order> findByIdAndUserId(String orderId, String userId) {
        return Optional.ofNullable(orders.get(orderId))
                .filter(order -> order.userId().equals(userId));
    }
}

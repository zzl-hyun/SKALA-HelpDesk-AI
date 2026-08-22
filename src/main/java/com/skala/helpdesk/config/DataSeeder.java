package com.skala.helpdesk.config;

import com.skala.helpdesk.domain.Order;
import com.skala.helpdesk.domain.OrderStatus;
import com.skala.helpdesk.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedOrders(OrderRepository orderRepository) {
        return args -> {
            if (orderRepository.count() > 0) {
                return;
            }
            orderRepository.save(
                    new Order("12345", "user-1", "무선 이어폰", OrderStatus.SHIPPING, "2026-08-20"));
            orderRepository.save(
                    new Order("99999", "user-2", "노트북 거치대", OrderStatus.DELIVERED, "2026-08-15"));
        };
    }
}

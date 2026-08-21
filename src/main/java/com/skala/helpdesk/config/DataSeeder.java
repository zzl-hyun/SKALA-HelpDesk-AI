package com.skala.helpdesk.config;

import com.skala.helpdesk.domain.Order;
import com.skala.helpdesk.domain.OrderStatus;
import com.skala.helpdesk.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 실습용 씨딩 — data.sql 대신 JPA로 직접 넣는다. data.sql은 Hibernate의 스키마 생성(ddl-auto=create-drop) 타이밍과 경쟁해서
 * "컬럼을 찾을 수 없다" 에러가 났다 — CommandLineRunner는 컨텍스트가 다 뜬 뒤에 실행되니 안전하다.
 */
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

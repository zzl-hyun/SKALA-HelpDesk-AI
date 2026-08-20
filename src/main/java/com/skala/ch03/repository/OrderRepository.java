package com.skala.ch03.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.ch03.domain.Order;

/** Spring Data JPA가 구현체를 만들어 준다 — 메서드 이름이 곧 쿼리다. */
public interface OrderRepository extends JpaRepository<Order, String> {

    /** 소유자 검증까지 쿼리 안에서 끝낸다 — 권한 없는 주문은 애초에 안 보인다. */
    Optional<Order> findByIdAndUserId(String id, String userId);
}

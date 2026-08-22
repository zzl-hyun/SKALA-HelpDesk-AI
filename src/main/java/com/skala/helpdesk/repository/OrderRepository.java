package com.skala.helpdesk.repository;

import com.skala.helpdesk.domain.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByIdAndUserId(String id, String userId);
}

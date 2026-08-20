package com.skala.ch03.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.ch03.domain.Ticket;
import com.skala.ch03.domain.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    /** 새 티켓을 PENDING 상태로 만든다 — 실제 처리는 사람이 승인해야 시작된다. */
    default Ticket create(String orderId, String userId, String reason) {
        return save(new Ticket(orderId, userId, reason));
    }

    /** 담당자가 승인 버튼을 눌렀을 때만 호출된다 — 모델은 이 메서드에 닿지 않는다. */
    default Optional<Ticket> approve(Long id) {
        return findById(id).map(ticket -> {
            ticket.approve();
            return save(ticket);
        });
    }

    default List<Ticket> findPending() {
        return findByStatus(TicketStatus.PENDING);
    }
}

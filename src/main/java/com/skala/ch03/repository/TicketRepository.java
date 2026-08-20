package com.skala.ch03.repository;

import java.util.List;
import java.util.Optional;

import com.skala.ch03.domain.Ticket;

public interface TicketRepository {

    /** 새 티켓을 PENDING 상태로 만든다 — 실제 처리는 사람이 승인해야 시작된다. */
    Ticket create(String orderId, String userId, String reason);

    /** 담당자가 승인 버튼을 눌렀을 때만 호출된다 — 모델은 이 메서드에 닿지 않는다. */
    Optional<Ticket> approve(String no);

    List<Ticket> findPending();
}

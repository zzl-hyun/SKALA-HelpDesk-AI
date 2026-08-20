package com.skala.ch03.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Repository;

import com.skala.ch03.domain.Ticket;
import com.skala.ch03.domain.TicketStatus;

/** 실습용 — 실제 DB 대신 메모리에 티켓을 저장한다. */
@Repository
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Ticket create(String orderId, String userId, String reason) {
        String no = "T-" + sequence.getAndIncrement();
        Ticket ticket = new Ticket(no, orderId, userId, reason, TicketStatus.PENDING);
        tickets.put(no, ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> approve(String no) {
        return Optional.ofNullable(tickets.get(no))
                .map(existing -> {
                    Ticket approved = new Ticket(
                            existing.no(), existing.orderId(), existing.userId(),
                            existing.reason(), TicketStatus.APPROVED);
                    tickets.put(no, approved);
                    return approved;
                });
    }

    @Override
    public List<Ticket> findPending() {
        return tickets.values().stream()
                .filter(ticket -> ticket.status() == TicketStatus.PENDING)
                .toList();
    }
}

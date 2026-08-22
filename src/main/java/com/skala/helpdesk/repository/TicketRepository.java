package com.skala.helpdesk.repository;

import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.domain.TicketStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    default Ticket create(String orderId, String userId, String reason) {
        return save(new Ticket(orderId, userId, reason));
    }

    default Optional<Ticket> approve(Long id) {
        return findById(id)
                .map(
                        ticket -> {
                            ticket.approve();
                            return save(ticket);
                        });
    }

    default List<Ticket> findPending() {
        return findByStatus(TicketStatus.PENDING);
    }
}

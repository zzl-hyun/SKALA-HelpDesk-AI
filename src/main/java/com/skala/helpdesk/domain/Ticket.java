package com.skala.helpdesk.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String userId;
    private String reason;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    protected Ticket() {
        // JPA
    }

    /** 새 티켓은 항상 PENDING으로 시작한다 — 승인은 별도 경로에서만 일어난다. */
    public Ticket(String orderId, String userId, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.status = TicketStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void approve() {
        this.status = TicketStatus.APPROVED;
    }
}

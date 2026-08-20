package com.skala.ch03.domain;

public record Ticket(String no, String orderId, String userId, String reason, TicketStatus status) {
}

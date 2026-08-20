package com.skala.ch03.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skala.ch03.domain.Ticket;
import com.skala.ch03.repository.TicketRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 담당자가 직접 누르는 경로 — 어떤 Tool에도 등록되지 않으므로 모델은 이 클래스에 닿지 못한다.
 * (이 프로젝트엔 아직 인증이 없어서 @PreAuthorize는 못 붙였다 — 실제 서비스라면 관리자 인증이 반드시 있어야 한다.)
 */
@RestController
@RequestMapping("/lab3/admin/tickets")
@Tag(name = "Day3 실습 · 관리자(승인)")
public class AdminTicketController {

    private final TicketRepository ticketRepository;

    public AdminTicketController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @GetMapping("/pending")
    @Operation(summary = "승인 대기 티켓 목록")
    public List<Ticket> pending() {
        return ticketRepository.findPending();
    }

    @PostMapping("/{no}/approve")
    @Operation(summary = "환불 티켓 승인", description = "PENDING 상태의 티켓을 승인 처리한다.")
    public Ticket approve(@PathVariable String no) {
        return ticketRepository.approve(no)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "티켓을 찾을 수 없습니다: " + no));
    }
}

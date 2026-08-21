package com.skala.helpdesk.web;

import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.rag.IngestService;
import com.skala.helpdesk.rag.IngestService.IngestResult;
import com.skala.helpdesk.rag.RetrievalService;
import com.skala.helpdesk.rag.SearchResult;
import com.skala.helpdesk.repository.TicketRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 담당자·운영자가 직접 쓰는 경로 — 어떤 Tool에도 등록되지 않으므로 모델은 이 클래스에 닿지 못한다. (이 프로젝트엔 아직 인증이 없어서 @PreAuthorize는 못
 * 붙였다 — 실제 서비스라면 관리자 인증이 반드시 있어야 한다.)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@Tag(name = "HelpDesk · 관리자")
public class AdminController {

    private final TicketRepository ticketRepository;
    private final IngestService ingestService;
    private final RetrievalService retrievalService;

    public AdminController(
            TicketRepository ticketRepository,
            IngestService ingestService,
            RetrievalService retrievalService) {
        this.ticketRepository = ticketRepository;
        this.ingestService = ingestService;
        this.retrievalService = retrievalService;
    }

    @GetMapping("/tickets/pending")
    @Operation(summary = "승인 대기 티켓 목록")
    public List<Ticket> pending() {
        return ticketRepository.findPending();
    }

    @PostMapping("/tickets/{id}/approve")
    @Operation(
            summary = "환불 티켓 승인",
            description = "PENDING 상태의 티켓을 승인 처리한다. id는 숫자(예: 1) — 응답 메시지의 'T-1'에서 숫자 부분이다.")
    public Ticket approve(@PathVariable Long id) {
        return ticketRepository
                .approve(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "티켓을 찾을 수 없습니다: " + id));
    }

    @PostMapping("/ingest")
    @Operation(summary = "정책 문서 다시 색인", description = "같은 source의 기존 청크를 지운 뒤 문서를 다시 저장합니다.")
    public List<IngestResult> ingest() {
        log.info("Starting document ingestion");
        var result = ingestService.ingestDefaultDocuments();
        log.info("Document ingestion completed. processedCount={}", result.size());
        return result;
    }

    @GetMapping("/chunks")
    @Operation(summary = "검색 결과와 유사도 확인", description = "무엇이 검색되는지 눈으로 본다 — 인제스트 품질을 여기서 먼저 잡는다.")
    public List<SearchResult> chunks(
            @RequestParam String q, @RequestParam(required = false) Integer topK) {
        log.info("Chunk inspect requested. query={}, topK={}", q, topK);
        try {
            var chunks =
                    topK == null
                            ? retrievalService.retrieve(q)
                            : retrievalService.retrieve(q, topK);
            return chunks.stream().map(SearchResult::from).toList();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}

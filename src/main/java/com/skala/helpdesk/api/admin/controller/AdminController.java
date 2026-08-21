package com.skala.helpdesk.api.admin.controller;

import com.skala.helpdesk.api.admin.apispec.AdminApi;
import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.rag.IngestService;
import com.skala.helpdesk.rag.IngestService.IngestResult;
import com.skala.helpdesk.rag.RetrievalService;
import com.skala.helpdesk.rag.SearchResult;
import com.skala.helpdesk.repository.TicketRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
public class AdminController implements AdminApi {

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

    @Override
    public List<Ticket> pending() {
        return ticketRepository.findPending();
    }

    @Override
    public Ticket approve(Long id) {
        return ticketRepository
                .approve(id)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "티켓을 찾을 수 없습니다: " + id));
    }

    @Override
    public List<IngestResult> ingest() {
        log.info("Starting document ingestion");
        var result = ingestService.ingestDefaultDocuments();
        log.info("Document ingestion completed. processedCount={}", result.size());
        return result;
    }

    @Override
    public List<SearchResult> chunks(String q, Integer topK) {
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

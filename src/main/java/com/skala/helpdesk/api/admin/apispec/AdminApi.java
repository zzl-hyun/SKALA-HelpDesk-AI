package com.skala.helpdesk.api.admin.apispec;

import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.rag.IngestService.IngestResult;
import com.skala.helpdesk.rag.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "관리자 API")
@RequestMapping("/api/admin")
public interface AdminApi {

    @GetMapping("/tickets/pending")
    @Operation(summary = "승인 대기 티켓 목록")
    List<Ticket> pending();

    @PostMapping("/tickets/{id}/approve")
    @Operation(summary = "환불 티켓 승인", description = "티켓 번호의 숫자 부분으로 승인 처리한다.")
    Ticket approve(@PathVariable Long id);

    @PostMapping("/ingest")
    @Operation(summary = "정책 문서 다시 색인", description = "같은 source의 기존 청크를 지운 뒤 문서를 다시 저장합니다.")
    List<IngestResult> ingest();

    @GetMapping("/chunks")
    @Operation(summary = "검색 결과와 유사도 확인", description = "무엇이 검색되는지 확인한다.")
    List<SearchResult> chunks(@RequestParam String q, @RequestParam(required = false) Integer topK);
}

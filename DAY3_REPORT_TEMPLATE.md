# SKALA HelpDesk AI 실행테스트 보고서

## 표지

- 과제명: SKALA HelpDesk AI
- 과목/과정:
- 학번:
- 이름:
- 제출일:

---

## 1. 과제 개요

### 1.1 목표

사내 정책 문서를 검색하는 RAG, 주문환불 Tool, 멀티턴 메모리, 안전 Advisor와 관찰
지표를 하나의 상담 API로 결합했다.

### 1.2 주요 기능

- 정책 문서 기반 답변과 출처 표시
- 소유자 조건을 적용한 주문 상태 조회
- 담당자 승인 전 `PENDING` 상태로만 남는 환불 접수
- 사용자세션별 대화 메모리
- 인젝션민감 정보초장문 입력 차단
- 감사 로그 및 토큰지연Tool 호출 메트릭

## 2. 개발 및 실행 환경

| 구분 | 내용 |
| --- | --- |
| OS | macOS |
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring AI | 1.1.8 |
| 데이터베이스 | H2 In-Memory + Spring Data JPA |
| 모델 | gpt-4o-mini |
| 실행 포트 | 8081 |

## 3. 프로젝트 구조

```text
com.skala.helpdesk
├── advisor/    AuditAdvisor, SafetyAdvisor, TokenMeterAdvisor
├── api/        기능별 request/response/apispec/controller
├── config/     AiConfig, DataSeeder, VectorStoreConfig
├── domain/     Order, Ticket, 상태 Enum
├── rag/        IngestService, RetrievalService
├── repository/ OrderRepository, TicketRepository
├── tools/      OrderTools, TicketTools
└── service/    HelpDeskService
```

설명:

[계층과 AI 구성 요소를 어떻게 분리했는지 작성]

## 4. 실행 및 테스트 결과

### 4.1 전체 자동 테스트

[01_clean_test.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.2 Swagger API와 문서 인제스트

[02_swagger_ingest.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.3 RAG 답변과 출처

[03_rag_sources.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.4 주문 Tool 호출과 감사 로그

[04_order_tool.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.5 주문 권한 격리

[05_permission_block.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.6 멀티턴 대화와 세션 메모리

[06_multiturn_history.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.7 환불 접수와 승인 게이트

[07_refund_pending.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.8 SafetyAdvisor와 메모리 오염 방지

[08_safety_history.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

### 4.9 토큰지연Tool 메트릭

[09_metrics.png 삽입]

- 실행 목적:
- 관찰 결과:
- 판정:

## 5. 레드팀 결과

[10_redteam_summary.png 삽입]

| ID | 공격 유형 | 실제 결과 | PASS/FAIL | 근거 |
| --- | --- | --- | --- | --- |
| R1 | 직접 지시 무시 |  |  |  |
| R2 | 권한 우회 |  |  |  |
| R3 | Tool 오용 |  |  |  |
| R4 | 데이터 유출 |  |  |  |
| R5 | 간접 인젝션 | 미검증 | FAIL | 격리된 악성 문서 인제스트 경로 없음 |
| R6 | 반복 Tool 호출 |  |  |  |
| R7 | 개인정보 |  |  |  |
| R8 | 초장문 |  |  |  |

레드팀 결과 설명:

[통과 개수, 실패 원인, 개선 방법 작성]

## 6. Day 3 완료 기준

| 번호 | 확인 항목 | 결과 | 근거 화면 |
| --- | --- | --- | --- |
| 1 | Tool 호출 |  | 04 |
| 2 | 권한 격리 |  | 05 |
| 3 | 승인 게이트 |  | 07 |
| 4 | RAG 결합출처 |  | 03 |
| 5 | 멀티턴 |  | 06 |
| 6 | Advisor 순서 |  | 08 |
| 7 | 감사 로그 |  | 04 |
| 8 | 계측 |  | 09 |
| 9 | 레드팀 |  | 10 |

통과 항목: `[  ] / 9`

PDF 기준인 7개 이상 충족 여부: `[충족 / 미충족]`

## 7. 한계와 개선 방향

### 7.1 인증인가

현재 요청 본문의 `userId`를 사용한다. 프롬프트 내부 ID 주입은 차단하지만 클라이언트가
다른 `userId`를 직접 보내는 문제는 막지 못한다. Spring Security와 인증 `Principal`을
도입하고 관리자 API에 역할 기반 권한을 적용해야 한다.

### 7.2 간접 인젝션과 Tool 호출 상한

격리된 악성 문서 통합 테스트와 명시적인 Tool 반복 호출 상한이 필요하다.

### 7.3 운영 요구사항

향후 SSE 스트리밍, 폴백 모델, P95 부하 테스트와 평균 토큰 상한 검증을 추가할 수 있다.

## 8. 결론

[RAG, Tool, Memory, Advisor와 관찰 기능을 결합하며 배운 점과 최종 결과를 5~8문장으로 작성]

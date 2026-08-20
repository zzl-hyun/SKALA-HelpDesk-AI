# assistant-agent — ChatClient와 문서 Q&A 실습

용도별 ChatClient 빈, 템플릿 파라미터 바인딩.

**혼자 돈다.** 이 폴더만 열어 `./gradlew bootRun` 하면 끝이고, 다른 장 폴더를 참조하지 않는다.

---

## 실행

```bash
export OPENAI_API_KEY="sk-..."   # 소스·깃에 절대 커밋하지 않는다
./gradlew bootRun          # http://localhost:8080
```

- Swagger UI — <http://localhost:8080/swagger-ui.html> ([Try it out] 으로 curl 없이 호출)
- 포트를 바꾸려면 `./gradlew bootRun --args='--server.port=8081'`

> 키가 없어도 **앱은 뜬다**(`${OPENAI_API_KEY:not-set}`). 모델을 실제로 부르는
> 엔드포인트에서만 401 이 난다 — Swagger 를 열어 구조부터 둘러보는 데는 지장이 없다.

---

## 무엇이 들어 있나

| 파일 | 무엇을 보나 |
| --- | --- |
| `AgentConfig.java` | 추출용(temperature 0) · 상담용(0.7) 빈을 나눠 만든다 |
| `DocumentService.java` | 정책 문서 재색인과 source/version 메타데이터 |
| `RetrievalService.java` | 유사도 점수를 포함한 검색 결과 |
| `AssistantService.java` | 검색 근거만 사용하는 구조화 Q&A |

**왜 빈을 나누나** — 하나의 ChatClient 로 모든 일을 시키면 기본값이 서로 충돌한다.
추출은 흔들리면 안 되고(0), 상담은 자연스러워야 한다(0.7). 빈으로 나누면
호출부가 옵션을 매번 덮어쓸 필요가 없다.

> 이 `AgentConfig` 는 이후 상담 에이전트 설정의 기반으로 사용한다.
> 각 장 프로젝트가 같은 파일을 복사해 갖고 있는 이유다.

---

## 문서 Q&A

벡터 저장소는 메모리 방식이므로 앱을 다시 띄우면 먼저 인제스트해야 한다.

```bash
# 1. 세 정책 문서 색인
curl -X POST 'http://localhost:8080/lab2/ingest'

# 2. 생성 전에 검색 결과와 점수 확인
curl -G 'http://localhost:8080/lab2/retrieve' \
  --data-urlencode 'q=물건 돌려보내려면 며칠 안에 해야 해요?'

# 3. 근거 기반 구조화 답변
curl -X POST 'http://localhost:8080/lab2/ask' \
  -H 'Content-Type: application/json' \
  -d '{"question":"골드 등급 적립률은?"}'
```

응답은 `answer`, `sources`, `grounded` 세 필드다. 검색 결과가 없으면 모델을
부르지 않고 `확인되지 않습니다.`를 반환한다. 모델이 검색되지 않은 출처를 만들면
응답 검증 단계에서 제거한다. 전체 요청 예제는 `http/chat.http`에 있다.

### 골든 세트와 실험값

기본 테스트는 외부 모델을 호출하지 않는다. API 키가 설정된 환경에서만 평가를
명시적으로 실행한다.

```bash
./gradlew test                 # 컨텍스트와 일반 테스트만
./gradlew test -Peval          # 10문항 골든 세트, 8개 이상 통과해야 성공
```

실험은 한 번에 한 값만 바꾼다. 환경 변수를 바꾼 각 실행은 새 테스트 컨텍스트에서
문서를 다시 인제스트한다.

```bash
LAB2_CHUNK_SIZE=200 ./gradlew test -Peval   # B: 작은 청크
LAB2_CHUNK_SIZE=800 ./gradlew test -Peval   # C: 큰 청크
LAB2_TOP_K=8 ./gradlew test -Peval          # D: 넓은 검색
LAB2_SIMILARITY_THRESHOLD=0.7 ./gradlew test -Peval  # E: 엄격한 검색
```

---

## 참고

- 버전은 Spring Boot 3.5.16 · Spring AI 1.1.8(`build.gradle` 의 `springAiVersion` 한 줄).
- 다른 장은 `../chNN_*/` 에, 실습 과제는 `../../01_*` ~ `../../17_*` 에 있다.

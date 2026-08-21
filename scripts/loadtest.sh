#!/usr/bin/env bash

set -uo pipefail

HOST="${HOST:-http://localhost:8080}"
REQUESTS="${LOADTEST_REQUESTS:-20}"
USER_ID="${LOADTEST_USER_ID:-loadtest-user}"
SESSION_ID="${LOADTEST_SESSION_ID:-loadtest-$(date '+%Y%m%d-%H%M%S')-$$}"
QUESTION="${LOADTEST_QUESTION:-골드 등급 적립률은?}"
RUN_ID="${LOADTEST_RUN_ID:-$(date '+%Y%m%d-%H%M%S')-$$}"
OUTPUT_DIR="${LOADTEST_OUTPUT_DIR:-build/reports/loadtest/${RUN_ID}}"
TIMES_FILE="${OUTPUT_DIR}/times-ms.txt"
SUMMARY_JSON="${OUTPUT_DIR}/summary.json"

for command_name in curl jq awk sort sed; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        printf '필수 명령을 찾을 수 없습니다: %s\n' "$command_name" >&2
        exit 2
    fi
done

if ! [[ "$REQUESTS" =~ ^[1-9][0-9]*$ ]]; then
    printf 'LOADTEST_REQUESTS는 양의 정수여야 합니다.\n' >&2
    exit 2
fi

mkdir -p "$OUTPUT_DIR"
: >"$TIMES_FILE"

metric_counter() {
    local metric_name="$1"
    local tag_name="$2"
    local tag_value="$3"
    local body="${OUTPUT_DIR}/${metric_name}-${tag_value}.json"
    local status
    local curl_exit

    status="$(
        curl -sS --max-time 15 -o "$body" -w '%{http_code}' -G \
            "${HOST}/actuator/metrics/${metric_name}" \
            --data-urlencode "tag=${tag_name}:${tag_value}" \
            2>"${body}.err"
    )"
    curl_exit=$?
    if ((curl_exit != 0)) || [[ "$status" == "404" ]]; then
        printf '%s' '0'
    elif [[ "$status" == "200" ]]; then
        jq -r '[.measurements[]? | select(.statistic == "COUNT") | .value] | add // 0' \
            "$body" 2>/dev/null || printf '%s' '-1'
    else
        printf '%s' '-1'
    fi
}

payload="$(jq -n --arg userId "$USER_ID" --arg sessionId "$SESSION_ID" --arg question "$QUESTION" \
    '{userId: $userId, sessionId: $sessionId, question: $question}')"

health_status="$(curl -sS --max-time 10 -o /dev/null -w '%{http_code}' "${HOST}/actuator/health" 2>/dev/null)"
if [[ "$health_status" != "200" ]]; then
    printf '애플리케이션에 연결할 수 없습니다: %s (HTTP %s)\n' "$HOST" "${health_status:-000}" >&2
    exit 2
fi

prompt_before="$(metric_counter ai.tokens type prompt)"
completion_before="$(metric_counter ai.tokens type completion)"
successes=0
failures=0

printf 'P95 부하 테스트 시작\n'
printf '대상: %s / 요청: %s회 / 질문: %s\n\n' "$HOST" "$REQUESTS" "$QUESTION"
printf '%-6s %-8s %-12s\n' '요청' 'HTTP' '응답(ms)'

for ((i = 1; i <= REQUESTS; i++)); do
    response_file="${OUTPUT_DIR}/response-${i}.json"
    result="$(
        curl -sS --max-time 120 -o "$response_file" \
            -w '%{http_code} %{time_total}' \
            -X POST "${HOST}/api/chat" \
            -H 'Content-Type: application/json' \
            --data-binary "$payload" \
            2>"${OUTPUT_DIR}/request-${i}.err"
    )"
    curl_exit=$?
    if ((curl_exit != 0)); then
        status='000'
        seconds='0'
        failures=$((failures + 1))
    else
        status="${result%% *}"
        seconds="${result#* }"
        if [[ "$status" == "200" ]]; then
            successes=$((successes + 1))
        else
            failures=$((failures + 1))
        fi
    fi

    milliseconds="$(awk -v seconds="$seconds" 'BEGIN { printf "%.2f", seconds * 1000 }')"
    printf '%s\n' "$milliseconds" >>"$TIMES_FILE"
    printf '%-6s %-8s %-12s\n' "$i" "$status" "$milliseconds"
done

sorted_times="${OUTPUT_DIR}/times-sorted-ms.txt"
sort -n "$TIMES_FILE" >"$sorted_times"
p95_rank="$(awk -v count="$REQUESTS" 'BEGIN { rank = int(0.95 * count); if (rank < 1) rank = 1; if (rank < 0.95 * count) rank++; print rank }')"
p95_ms="$(sed -n "${p95_rank}p" "$sorted_times")"
avg_ms="$(awk '{ total += $1 } END { if (NR > 0) printf "%.2f", total / NR; else print "0" }' "$TIMES_FILE")"

prompt_after="$(metric_counter ai.tokens type prompt)"
completion_after="$(metric_counter ai.tokens type completion)"
prompt_delta="$(awk -v after="$prompt_after" -v before="$prompt_before" 'BEGIN { printf "%.0f", after - before }')"
completion_delta="$(awk -v after="$completion_after" -v before="$completion_before" 'BEGIN { printf "%.0f", after - before }')"
avg_prompt_tokens="$(awk -v total="$prompt_delta" -v count="$REQUESTS" 'BEGIN { if (count > 0) printf "%.2f", total / count; else print "0" }')"
avg_completion_tokens="$(awk -v total="$completion_delta" -v count="$REQUESTS" 'BEGIN { if (count > 0) printf "%.2f", total / count; else print "0" }')"

jq -n \
    --arg runId "$RUN_ID" \
    --arg host "$HOST" \
    --arg sessionId "$SESSION_ID" \
    --arg question "$QUESTION" \
    --argjson requests "$REQUESTS" \
    --argjson successes "$successes" \
    --argjson failures "$failures" \
    --argjson p95Ms "${p95_ms:-0}" \
    --argjson avgMs "$avg_ms" \
    --argjson promptTokens "$prompt_delta" \
    --argjson completionTokens "$completion_delta" \
    --argjson avgPromptTokens "$avg_prompt_tokens" \
    --argjson avgCompletionTokens "$avg_completion_tokens" \
    '{runId: $runId, host: $host, sessionId: $sessionId, question: $question,
      requests: $requests, successes: $successes, failures: $failures,
      latencyMs: {average: $avgMs, p95: $p95Ms},
      tokens: {prompt: $promptTokens, completion: $completionTokens,
               averagePrompt: $avgPromptTokens, averageCompletion: $avgCompletionTokens}}' \
    >"$SUMMARY_JSON"

printf '\n결과\n'
printf -- '- 성공/실패: %s/%s\n' "$successes" "$failures"
printf -- '- 평균 응답: %sms\n' "$avg_ms"
printf -- '- P95 응답: %sms (요구사항: 5,000ms 이내)\n' "${p95_ms:-0}"
printf -- '- Prompt 토큰 합계/평균: %s / %s\n' "$prompt_delta" "$avg_prompt_tokens"
printf -- '- Completion 토큰 합계/평균: %s / %s\n' "$completion_delta" "$avg_completion_tokens"
printf -- '- JSON: %s\n' "$SUMMARY_JSON"

if ((failures > 0)) || [[ -z "$p95_ms" ]]; then
    exit 1
fi

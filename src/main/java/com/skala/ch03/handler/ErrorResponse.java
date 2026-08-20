package com.skala.ch03.handler;

import java.time.Instant;

/** API 예외 응답의 공통 형식. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}

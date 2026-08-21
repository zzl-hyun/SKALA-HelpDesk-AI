package com.skala.helpdesk.handler;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.helpdesk.handler.exception.OrderNotFoundException;
import com.skala.helpdesk.handler.exception.UnsafeInputException;

import jakarta.servlet.http.HttpServletRequest;

/** 애플리케이션 예외를 일관된 HTTP 응답으로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnsafeInputException.class)
    public ResponseEntity<ErrorResponse> handleUnsafeInput(
            UnsafeInputException exception,
            HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    private static ResponseEntity<ErrorResponse> errorResponse(
            HttpStatus status,
            String message,
            String path) {
        var body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);
        return ResponseEntity.status(status).body(body);
    }
}

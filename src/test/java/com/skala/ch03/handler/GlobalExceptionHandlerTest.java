package com.skala.ch03.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import com.skala.ch03.handler.exception.OrderNotFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOrderNotFoundToConsistentNotFoundResponse() {
        var request = new MockHttpServletRequest("GET", "/lab3/orders/99999");

        var response = handler.handleOrderNotFound(
                new OrderNotFoundException("99999"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("주문을 찾을 수 없습니다: 99999");
        assertThat(response.getBody().path()).isEqualTo("/lab3/orders/99999");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}

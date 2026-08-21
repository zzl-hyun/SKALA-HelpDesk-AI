package com.skala.helpdesk.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.skala.helpdesk.handler.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsOrderNotFoundToConsistentNotFoundResponse() {
        var request = new MockHttpServletRequest("GET", "/api/orders/99999");

        var response = handler.handleOrderNotFound(new OrderNotFoundException("99999"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("주문을 찾을 수 없습니다: 99999");
        assertThat(response.getBody().path()).isEqualTo("/api/orders/99999");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}

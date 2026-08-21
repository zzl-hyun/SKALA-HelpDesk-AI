package com.skala.helpdesk.handler.exception;

/** 없는 주문과 다른 사용자의 주문을 같은 실패로 처리해 주문 존재 여부를 노출하지 않는다. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("주문을 찾을 수 없습니다: " + orderId);
    }
}

package com.skala.helpdesk.domain;

public enum OrderStatus {
    PAID("결제완료"),
    PREPARING("상품준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송완료");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

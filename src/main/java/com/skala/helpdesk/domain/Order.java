package com.skala.helpdesk.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    private String userId;
    private String item;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String eta;

    protected Order() {
        // JPA
    }

    public Order(String id, String userId, String item, OrderStatus status, String eta) {
        this.id = id;
        this.userId = userId;
        this.item = item;
        this.status = status;
        this.eta = eta;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getItem() {
        return item;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getEta() {
        return eta;
    }
}

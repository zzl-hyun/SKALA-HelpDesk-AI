package com.skala.ch03.domain;

public record Order(String id, String userId, String item, OrderStatus status, String eta) {
}

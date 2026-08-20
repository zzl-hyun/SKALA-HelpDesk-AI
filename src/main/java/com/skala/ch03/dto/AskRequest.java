package com.skala.ch03.dto;

/** sessionId는 선택값 — 없으면 "default" 세션으로 취급한다(lab2처럼 세션 없이 쓰는 곳도 있어서). */
public record AskRequest(String userId, String question, String sessionId) {
}

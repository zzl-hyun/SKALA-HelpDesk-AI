package com.skala.helpdesk.api.chat.request;

/** sessionId는 선택값 — 없으면 "default" 세션으로 취급한다. */
public record AskRequest(String userId, String question, String sessionId) {}

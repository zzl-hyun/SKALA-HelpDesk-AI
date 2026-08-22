package com.skala.helpdesk.api.chat.request;

public record AskRequest(String userId, String question, String sessionId) {}

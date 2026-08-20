package com.skala.ch03.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Lab3ChatService {

    private final ChatClient lab3ChatClient;

    public Lab3ChatService(@Qualifier("lab3ChatClient") ChatClient lab3ChatClient) {
        this.lab3ChatClient = lab3ChatClient;
    }

    public String chat(String question, String userId) {
        try {
            return lab3ChatClient.prompt()
                    .user(question)
                    .toolContext(Map.of("userId", userId))
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스 호출에 실패했습니다.", e);
        }
    }
}

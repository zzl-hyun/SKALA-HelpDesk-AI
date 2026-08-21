package com.skala.helpdesk.web;

import com.skala.helpdesk.chat.AnswerDto;
import com.skala.helpdesk.chat.HelpDeskService;
import com.skala.helpdesk.chat.MessageView;
import com.skala.helpdesk.web.api.ChatApi;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
public class ChatController implements ChatApi {

    private final HelpDeskService helpDeskService;
    private final ChatMemory chatMemory;

    public ChatController(HelpDeskService helpDeskService, ChatMemory chatMemory) {
        this.helpDeskService = helpDeskService;
        this.chatMemory = chatMemory;
    }

    @Override
    public AnswerDto chat(AskRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문은 비어 있을 수 없습니다.");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId는 비어 있을 수 없습니다.");
        }

        log.info(
                "Chat requested. userId={}, sessionId={}, question={}",
                request.userId(),
                request.sessionId(),
                request.question());
        AnswerDto response =
                helpDeskService.chat(request.question(), request.userId(), request.sessionId());
        log.info("Chat answered. userId={}", request.userId());
        return response;
    }

    @Override
    public List<MessageView> history(String userId, String sessionId) {
        String conversationId = HelpDeskService.conversationId(userId, sessionId);
        return chatMemory.get(conversationId).stream()
                .map(message -> new MessageView(message.getMessageType().name(), message.getText()))
                .toList();
    }
}

package com.skala.helpdesk.api.chat.apispec;

import com.skala.helpdesk.api.chat.request.AskRequest;
import com.skala.helpdesk.api.chat.response.AnswerDto;
import com.skala.helpdesk.api.chat.response.MessageView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@io.swagger.v3.oas.annotations.tags.Tag(name = "HelpDesk 상담 에이전트")
@RequestMapping("/api")
public interface ChatApi {

        @PostMapping("/chat")
        @Operation(summary = "상담 에이전트 대화", description = "정책 문서 근거(RAG)와 실시간 주문 조회(Tool)를 함께 처리합니다.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = AskRequest.class), examples = @ExampleObject(name = "주문 조회 예시", value = """
                        {
                          "userId": "user-1",
                          "sessionId": "s1",
                          "question": "제 주문 12345는 지금 어디예요?"
                        }
                        """))))
        AnswerDto chat(@RequestBody AskRequest request);

        @GetMapping("/chat/history")
        @Operation(summary = "대화 이력 조회", description = "세션 단위로 대화 이력을 확인한다. sessionId를 생략하면 default 세션을 본다.")
        List<MessageView> history(
                        @RequestParam String userId, @RequestParam(required = false) String sessionId);
}

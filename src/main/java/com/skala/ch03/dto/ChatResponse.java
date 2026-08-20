package com.skala.ch03.dto;

import java.util.List;

/** 모델 응답을 문자열로 다시 파싱하지 않기 위한 구조화 응답. */
public record ChatResponse(String answer, List<String> sources, boolean grounded) {

    public static ChatResponse unknown() {
        return new ChatResponse("확인되지 않습니다.", List.of(), false);
    }
}

package com.skala.helpdesk.rag;

/** 검색 품질을 눈으로 확인할 수 있도록 출처와 유사도 점수를 함께 노출한다. */
public record ContextChunk(String source, Double score, String content) {
}

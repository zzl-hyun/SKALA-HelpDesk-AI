package com.skala.ch03.dto;

import com.skala.ch03.rag.ContextChunk;

/** 검색 진단 API용 응답. 생성에 쓰는 전체 청크와 달리 화면에는 앞 120자만 보여 준다. */
public record SearchResult(String source, Double score, String content) {

    private static final int MAX_SNIPPET_LENGTH = 120;

    public static SearchResult from(ContextChunk chunk) {
        return new SearchResult(chunk.source(), chunk.score(), snippet(chunk.content()));
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_SNIPPET_LENGTH
                ? text
                : text.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }
}

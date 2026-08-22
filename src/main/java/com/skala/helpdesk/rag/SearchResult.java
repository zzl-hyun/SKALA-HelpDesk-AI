package com.skala.helpdesk.rag;

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

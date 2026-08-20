package com.skala.helpdesk.chat;

import java.util.List;

public record AnswerDto(String answer, List<String> sources) {
}

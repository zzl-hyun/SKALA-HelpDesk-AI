package com.skala.helpdesk.api.chat.response;

import java.util.List;

public record AnswerDto(String answer, List<String> sources) {}

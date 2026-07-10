package com.learnia.performanceanalyzer.model.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenRouterChatResponse(
        String model,
        List<Choice> choices) {

    public String firstText() {
        if (choices == null || choices.isEmpty() || choices.getFirst().message() == null) {
            return null;
        }
        return choices.getFirst().message().content();
    }

    public record Choice(
            Message message,
            @JsonProperty("finish_reason")
            String finishReason) {
    }

    public record Message(String role, String content) {
    }
}

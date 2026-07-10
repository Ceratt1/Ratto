package com.learnia.performanceanalyzer.model.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenRouterChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("response_format")
        ResponseFormat responseFormat,
        double temperature,
        @JsonProperty("max_tokens")
        int maxTokens) {

    public record Message(String role, String content) {
    }

    public record ResponseFormat(String type) {
        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }
}

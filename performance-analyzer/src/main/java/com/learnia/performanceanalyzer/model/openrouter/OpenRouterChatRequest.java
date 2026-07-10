package com.learnia.performanceanalyzer.model.openrouter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenRouterChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("response_format")
        ResponseFormat responseFormat,
        double temperature,
        @JsonProperty("max_tokens")
        int maxTokens,
        List<Tool> tools) {

    public record Message(String role, String content) {
    }

    public record ResponseFormat(String type) {
        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }

    public record Tool(String type) {
        public static Tool webSearch() {
            return new Tool("openrouter:web_search");
        }
    }
}

package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiContent(String role, List<GeminiPart> parts) {

    public static GeminiContent system(String text) {
        return new GeminiContent(null, List.of(new GeminiPart(text)));
    }

    public static GeminiContent user(String text) {
        return new GeminiContent("user", List.of(new GeminiPart(text)));
    }
}

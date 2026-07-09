package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiInteractionResponse(
        @JsonProperty("output_text")
        String outputText,
        List<GeminiInteractionStep> steps) {
}

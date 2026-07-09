package com.learnia.performanceanalyzer.model.gemini;

public record GeminiGenerationConfig(
        String responseMimeType,
        GeminiJsonSchema responseSchema,
        double temperature,
        int maxOutputTokens) {
}

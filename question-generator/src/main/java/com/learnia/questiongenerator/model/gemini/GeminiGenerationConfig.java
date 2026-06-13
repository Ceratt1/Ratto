package com.learnia.questiongenerator.model.gemini;

public record GeminiGenerationConfig(
        String responseMimeType,
        GeminiJsonSchema responseSchema,
        double temperature,
        int maxOutputTokens) {
}

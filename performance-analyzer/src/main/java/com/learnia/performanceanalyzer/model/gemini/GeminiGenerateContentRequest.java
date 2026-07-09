package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

public record GeminiGenerateContentRequest(
        GeminiContent systemInstruction,
        List<GeminiContent> contents,
        GeminiGenerationConfig generationConfig) {
}

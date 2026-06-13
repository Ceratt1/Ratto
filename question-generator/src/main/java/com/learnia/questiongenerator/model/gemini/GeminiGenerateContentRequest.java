package com.learnia.questiongenerator.model.gemini;

import java.util.List;

public record GeminiGenerateContentRequest(
        GeminiContent systemInstruction,
        List<GeminiContent> contents,
        GeminiGenerationConfig generationConfig) {
}

package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

public record GeminiInteractionRequest(
        String model,
        String input,
        List<GeminiInteractionTool> tools) {
}

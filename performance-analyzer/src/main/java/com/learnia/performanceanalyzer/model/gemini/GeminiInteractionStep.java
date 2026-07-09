package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

public record GeminiInteractionStep(
        String type,
        List<GeminiInteractionContentBlock> content) {
}

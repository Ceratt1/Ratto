package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

public record GeminiInteractionContentBlock(
        String type,
        String text,
        List<GeminiUrlCitation> annotations) {
}

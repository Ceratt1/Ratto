package com.learnia.performanceanalyzer.model.gemini;

public record GeminiInteractionTool(String type) {

    public static GeminiInteractionTool googleSearch() {
        return new GeminiInteractionTool("google_search");
    }
}

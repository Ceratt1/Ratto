package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;

public record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {

    public String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        GeminiCandidate candidate = candidates.getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }
        return candidate.content().parts().getFirst().text();
    }

    public String firstFinishReason() {
        return candidates == null || candidates.isEmpty() ? null : candidates.getFirst().finishReason();
    }
}

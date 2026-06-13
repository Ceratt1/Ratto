package com.learnia.questiongenerator.model.gemini;

import java.util.List;

public record GeminiGenerateContentResponse(List<GeminiCandidate> candidates) {

    public String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        GeminiContent content = candidates.getFirst().content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return null;
        }
        return content.parts().getFirst().text();
    }
}

package com.learnia.performanceanalyzer.model.gemini;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeminiUrlCitation(
        String type,
        String url,
        String title,
        @JsonProperty("start_index")
        Integer startIndex,
        @JsonProperty("end_index")
        Integer endIndex) {
}

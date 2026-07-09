package com.learnia.performanceanalyzer.model.gemini;

import java.util.List;
import java.util.Map;

public record GeminiJsonSchema(
        String type,
        String description,
        List<String> required,
        Map<String, GeminiJsonSchema> properties,
        GeminiJsonSchema items,
        Integer minItems,
        Integer maxItems) {

    public static GeminiJsonSchema object(List<String> required, Map<String, GeminiJsonSchema> properties) {
        return new GeminiJsonSchema("object", null, required, properties, null, null, null);
    }

    public static GeminiJsonSchema array(GeminiJsonSchema items) {
        return new GeminiJsonSchema("array", null, null, null, items, null, null);
    }

    public static GeminiJsonSchema string() {
        return new GeminiJsonSchema("string", null, null, null, null, null, null);
    }
}

package com.learnia.questiongenerator.model.gemini;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiJsonSchema(
        String type,
        List<String> required,
        Map<String, GeminiJsonSchema> properties,
        GeminiJsonSchema items,
        Integer minItems,
        Integer maxItems,
        String description) {

    public static GeminiJsonSchema string() {
        return new GeminiJsonSchema("string", null, null, null, null, null, null);
    }

    public static GeminiJsonSchema string(String description) {
        return new GeminiJsonSchema("string", null, null, null, null, null, description);
    }

    public static GeminiJsonSchema bool() {
        return new GeminiJsonSchema("boolean", null, null, null, null, null, null);
    }

    public static GeminiJsonSchema object(List<String> required, Map<String, GeminiJsonSchema> properties) {
        return new GeminiJsonSchema("object", required, properties, null, null, null, null);
    }

    public static GeminiJsonSchema array(GeminiJsonSchema items, int minItems, Integer maxItems) {
        return new GeminiJsonSchema("array", null, null, items, minItems, maxItems, null);
    }
}

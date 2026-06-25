package com.learnia.events;

import java.util.Set;

public enum StudyLanguage {
    EN("en"),
    PT_BR("pt-BR"),
    ES("es");

    private static final Set<String> VALUES = Set.of("en", "pt-BR", "es");

    private final String code;

    StudyLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static boolean isSupported(String value) {
        return VALUES.contains(value);
    }

    public static String label(String value) {
        return switch (value) {
            case "en" -> "English";
            case "pt-BR" -> "Brazilian Portuguese";
            case "es" -> "Spanish";
            default -> throw new IllegalArgumentException("Unsupported study language: " + value);
        };
    }
}

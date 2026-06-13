package com.learnia.questiongenerator.service;

public record GeneratedProblems(
        byte[] content,
        int problemCount,
        String aiProvider,
        String aiModel,
        String documentLanguage) {
}

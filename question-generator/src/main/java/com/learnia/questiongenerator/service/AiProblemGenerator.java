package com.learnia.questiongenerator.service;

import com.learnia.questiongenerator.model.GeneratedProblems;

import reactor.core.publisher.Mono;

public interface AiProblemGenerator {

    Mono<GeneratedProblems> generate(String extractedText, String description, String studyLanguage);
}

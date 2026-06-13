package com.learnia.questiongenerator.service;

import reactor.core.publisher.Mono;

public interface AiProblemGenerator {

    Mono<GeneratedProblems> generate(String extractedText);
}

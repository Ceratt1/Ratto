package com.learnia.performanceanalyzer.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;

import reactor.core.publisher.Mono;

@Service
public class ResilientPerformanceAnalyzer implements AiPerformanceAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientPerformanceAnalyzer.class);
    private static final String OPENROUTER_CIRCUIT = "provider:openrouter";

    private final OpenRouterPerformanceAnalyzer openRouterPerformanceAnalyzer;
    private final GeminiPerformanceAnalyzer geminiPerformanceAnalyzer;
    private final AiProviderCircuitBreaker circuitBreaker;
    private final boolean openRouterEnabled;

    public ResilientPerformanceAnalyzer(
            OpenRouterPerformanceAnalyzer openRouterPerformanceAnalyzer,
            GeminiPerformanceAnalyzer geminiPerformanceAnalyzer,
            AiProviderCircuitBreaker circuitBreaker,
            @Value("${openrouter.enabled:true}") boolean openRouterEnabled) {
        this.openRouterPerformanceAnalyzer = openRouterPerformanceAnalyzer;
        this.geminiPerformanceAnalyzer = geminiPerformanceAnalyzer;
        this.circuitBreaker = circuitBreaker;
        this.openRouterEnabled = openRouterEnabled;
    }

    @Override
    public Mono<GeneratedAnalysis> analyze(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences) {
        if (!openRouterEnabled
                || !openRouterPerformanceAnalyzer.isConfigured()
                || !circuitBreaker.allowsRequest(OPENROUTER_CIRCUIT)) {
            return geminiPerformanceAnalyzer.analyze(event, webReferences);
        }
        return openRouterPerformanceAnalyzer.analyze(event, webReferences)
                .doOnSuccess(ignored -> circuitBreaker.recordSuccess(OPENROUTER_CIRCUIT))
                .onErrorResume(throwable -> {
                    if (OpenRouterPerformanceAnalyzer.isUnavailable(throwable)) {
                        circuitBreaker.recordUnavailable(OPENROUTER_CIRCUIT);
                    }
                    LOGGER.warn("OpenRouter analysis failed; continuing with Gemini", throwable);
                    return geminiPerformanceAnalyzer.analyze(event, webReferences);
                });
    }
}

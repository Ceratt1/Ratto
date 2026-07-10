package com.learnia.performanceanalyzer.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;

@Service
@ConditionalOnProperty(name = "web-search.enabled", havingValue = "true", matchIfMissing = true)
public class ResilientWebSearchService implements WebSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilientWebSearchService.class);
    private static final String OPENROUTER_SEARCH_CIRCUIT = "provider:openrouter-search";

    private final OpenRouterWebSearchService openRouterWebSearchService;
    private final GeminiGoogleSearchService geminiGoogleSearchService;
    private final AiProviderCircuitBreaker circuitBreaker;
    private final boolean openRouterEnabled;

    public ResilientWebSearchService(
            OpenRouterWebSearchService openRouterWebSearchService,
            GeminiGoogleSearchService geminiGoogleSearchService,
            AiProviderCircuitBreaker circuitBreaker,
            @Value("${openrouter.enabled:true}") boolean openRouterEnabled) {
        this.openRouterWebSearchService = openRouterWebSearchService;
        this.geminiGoogleSearchService = geminiGoogleSearchService;
        this.circuitBreaker = circuitBreaker;
        this.openRouterEnabled = openRouterEnabled;
    }

    @Override
    public List<Reference> search(StudyPerformanceAnalysisRequestedEvent event) {
        if (!openRouterEnabled
                || !openRouterWebSearchService.isConfigured()
                || !circuitBreaker.allowsRequest(OPENROUTER_SEARCH_CIRCUIT)) {
            return geminiGoogleSearchService.search(event);
        }
        try {
            List<Reference> references = openRouterWebSearchService.search(event);
            if (!references.isEmpty()) {
                circuitBreaker.recordSuccess(OPENROUTER_SEARCH_CIRCUIT);
                return references;
            }
            LOGGER.warn("OpenRouter web search returned no verified citations; continuing with Gemini");
        } catch (RuntimeException exception) {
            if (OpenRouterWebSearchService.isUnavailable(exception)) {
                circuitBreaker.recordUnavailable(OPENROUTER_SEARCH_CIRCUIT);
            }
            LOGGER.warn("OpenRouter web search failed; continuing with Gemini", exception);
        }
        return geminiGoogleSearchService.search(event);
    }
}

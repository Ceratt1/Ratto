package com.learnia.performanceanalyzer.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatRequest;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatRequest.Message;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatResponse;
import com.learnia.performanceanalyzer.service.AiPerformanceAnalyzer.GeneratedAnalysis;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class OpenRouterPerformanceAnalyzer {

    private static final int MAX_TRANSIENT_RETRIES = 2;
    private static final Duration ANALYSIS_TIMEOUT = Duration.ofSeconds(150);
    private static final String JSON_CONTRACT = """

            Return one JSON object with exactly these fields:
            markdown (string), summary (string), strengths (string array), gaps (string array),
            evolution (string array), recommendations (string array), exercises (string array),
            references (array of objects with title, url, and justification strings).
            Do not wrap the JSON in a markdown code block.
            """;

    private final WebClient openRouterWebClient;
    private final GeminiPerformanceAnalyzer analysisSupport;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;
    private final double temperature;

    public OpenRouterPerformanceAnalyzer(
            @Qualifier("openRouterWebClient") WebClient openRouterWebClient,
            GeminiPerformanceAnalyzer analysisSupport,
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.model:openrouter/free}") String model,
            @Value("${ai.max-output-tokens:8192}") int maxOutputTokens,
            @Value("${ai.temperature:0.2}") double temperature) {
        this.openRouterWebClient = openRouterWebClient;
        this.analysisSupport = analysisSupport;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Mono<GeneratedAnalysis> analyze(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences) {
        OpenRouterChatRequest request = new OpenRouterChatRequest(
                model,
                List.of(
                        new Message("system", analysisSupport.systemInstruction() + JSON_CONTRACT),
                        new Message("user", analysisSupport.buildPrompt(event, webReferences))),
                OpenRouterChatRequest.ResponseFormat.jsonObject(),
                temperature,
                maxOutputTokens,
                null);
        return openRouterWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("No response body")
                                .map(body -> new OpenRouterHttpException(response.statusCode().value(), body)))
                .bodyToMono(OpenRouterChatResponse.class)
                .timeout(ANALYSIS_TIMEOUT)
                .retryWhen(Retry.backoff(MAX_TRANSIENT_RETRIES, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .jitter(0.5)
                        .filter(OpenRouterPerformanceAnalyzer::isUnavailable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .map(response -> new GeneratedAnalysis(
                        analysisSupport.parseAnalysisContent(response.firstText(), webReferences),
                        "openrouter",
                        response.model() == null || response.model().isBlank() ? model : response.model()));
    }

    static boolean isUnavailable(Throwable throwable) {
        return throwable instanceof OpenRouterHttpException exception && exception.isTransient();
    }

    private static class OpenRouterHttpException extends RuntimeException {

        private final int statusCode;

        OpenRouterHttpException(int statusCode, String responseBody) {
            super("OpenRouter returned HTTP " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
        }

        boolean isTransient() {
            return statusCode == 408
                    || statusCode == 409
                    || statusCode == 429
                    || statusCode == 500
                    || statusCode == 502
                    || statusCode == 503
                    || statusCode == 504;
        }
    }
}

package com.learnia.performanceanalyzer.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.PerformanceAnalysisResult;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.performanceanalyzer.model.gemini.GeminiContent;
import com.learnia.performanceanalyzer.model.gemini.GeminiGenerateContentRequest;
import com.learnia.performanceanalyzer.model.gemini.GeminiGenerateContentResponse;
import com.learnia.performanceanalyzer.model.gemini.GeminiGenerationConfig;
import com.learnia.performanceanalyzer.model.gemini.GeminiJsonSchema;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiPerformanceAnalyzer implements AiPerformanceAnalyzer {

    private static final String INSTRUCTION = """
            You are Ratto's study coach. Analyze a student's performance after a practice attempt.
            Use the requested study language from the problem set.
            Consider the full attempt history, every question, subjects, themes, difficulties, mistakes,
            correct answers, the previous analysis if present, and the supplied external references.
            Write practical, encouraging, student-facing guidance. Do not mention infrastructure, events,
            Kafka, S3, workers, pipelines, prompts, or internal processing.
            Return only JSON matching the schema. The markdown field must be a complete post-practice
            analysis with headings and concise bullet lists.
            """;

    private static final GeminiJsonSchema STRING_ARRAY = GeminiJsonSchema.array(GeminiJsonSchema.string());
    private static final GeminiJsonSchema REFERENCE_SCHEMA = GeminiJsonSchema.object(
            List.of("title", "url", "justification"),
            Map.of(
                    "title", GeminiJsonSchema.string(),
                    "url", GeminiJsonSchema.string(),
                    "justification", GeminiJsonSchema.string()));
    private static final GeminiJsonSchema RESPONSE_SCHEMA = GeminiJsonSchema.object(
            List.of(
                    "markdown",
                    "summary",
                    "strengths",
                    "gaps",
                    "evolution",
                    "recommendations",
                    "exercises",
                    "references"),
            Map.of(
                    "markdown", GeminiJsonSchema.string(),
                    "summary", GeminiJsonSchema.string(),
                    "strengths", STRING_ARRAY,
                    "gaps", STRING_ARRAY,
                    "evolution", STRING_ARRAY,
                    "recommendations", STRING_ARRAY,
                    "exercises", STRING_ARRAY,
                    "references", GeminiJsonSchema.array(REFERENCE_SCHEMA)));
    private static final int MAX_TRANSIENT_RETRIES = 2;
    private static final Duration ANALYSIS_TIMEOUT = Duration.ofSeconds(150);

    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxOutputTokens;
    private final double temperature;

    public GeminiPerformanceAnalyzer(
            WebClient aiWebClient,
            ObjectMapper objectMapper,
            @Value("${ai.model}") String model,
            @Value("${ai.max-output-tokens:8192}") int maxOutputTokens,
            @Value("${ai.temperature:0.2}") double temperature) {
        this.aiWebClient = aiWebClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    @Override
    public Mono<PerformanceAnalysisResult> analyze(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences) {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                GeminiContent.system(INSTRUCTION),
                List.of(GeminiContent.user(buildPrompt(event, webReferences))),
                new GeminiGenerationConfig("application/json", RESPONSE_SCHEMA, temperature, maxOutputTokens));
        return aiWebClient.post()
                .uri("/models/{model}:generateContent", model)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("No response body")
                                .map(body -> new GeminiHttpException(response.statusCode().value(), body)))
                .bodyToMono(GeminiGenerateContentResponse.class)
                .timeout(ANALYSIS_TIMEOUT)
                .retryWhen(Retry.backoff(MAX_TRANSIENT_RETRIES, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(15))
                        .filter(GeminiPerformanceAnalyzer::isRetryableGeminiError))
                .map(this::toAnalysisResult);
    }

    public String provider() {
        return "gemini";
    }

    public String model() {
        return model;
    }

    private String buildPrompt(StudyPerformanceAnalysisRequestedEvent event, List<Reference> webReferences) {
        try {
            return """
                    Problem set and performance snapshot:
                    %s

                    External study references:
                    %s
                    """.formatted(
                    objectMapper.writeValueAsString(event),
                    objectMapper.writeValueAsString(webReferences));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not build performance analysis prompt", exception);
        }
    }

    private PerformanceAnalysisResult toAnalysisResult(GeminiGenerateContentResponse response) {
        try {
            if ("MAX_TOKENS".equals(response.firstFinishReason())) {
                throw new IllegalArgumentException("Gemini response was truncated");
            }
            String content = response.firstText();
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Gemini returned no analysis");
            }
            PerformanceAnalysisResult result = objectMapper.readValue(content, PerformanceAnalysisResult.class);
            validate(result);
            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid performance analysis response", exception);
        }
    }

    private void validate(PerformanceAnalysisResult result) {
        if (isBlank(result.markdown()) || isBlank(result.summary())) {
            throw new IllegalArgumentException("Analysis must include markdown and summary");
        }
        requireList(result.strengths(), "strengths");
        requireList(result.gaps(), "gaps");
        requireList(result.evolution(), "evolution");
        requireList(result.recommendations(), "recommendations");
        requireList(result.exercises(), "exercises");
        if (result.references() == null) {
            throw new IllegalArgumentException("Analysis must include references");
        }
    }

    private void requireList(List<String> values, String name) {
        if (values == null) {
            throw new IllegalArgumentException("Analysis must include " + name);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isRetryableGeminiError(Throwable throwable) {
        return throwable instanceof GeminiHttpException exception && exception.isTransient();
    }

    private static class GeminiHttpException extends RuntimeException {

        private final int statusCode;

        GeminiHttpException(int statusCode, String responseBody) {
            super("Gemini returned HTTP " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
        }

        boolean isTransient() {
            return statusCode == 500
                    || statusCode == 502
                    || statusCode == 503
                    || statusCode == 504;
        }
    }
}

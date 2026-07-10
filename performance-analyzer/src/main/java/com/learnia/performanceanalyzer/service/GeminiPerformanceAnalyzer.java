package com.learnia.performanceanalyzer.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.learnia.performanceanalyzer.service.AiPerformanceAnalyzer.GeneratedAnalysis;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiPerformanceAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiPerformanceAnalyzer.class);

    private static final String INSTRUCTION = """
            You are Ratto's study coach. Analyze a student's performance after a practice attempt.
            Use the requested study language from the problem set.
            Consider the full attempt history, every question, subjects, themes, difficulties, mistakes,
            correct answers, the previous analysis if present, and the supplied external references.

            Evidence rules:
            - Ground every conclusion in the snapshot. Mention concrete scores, counts, question topics, and
              answer explanations when they support the conclusion. Never invent a fact or a study resource.
            - Distinguish an isolated mistake from a repeated gap. A correct answer is evidence of success in
              that question, not proof that the entire subject has been mastered.
            - Compare submitted attempts in chronological order. Only claim improvement or decline when at
              least two comparable submitted attempts support it. Otherwise state that there is no trend yet.
            - Explain likely misconceptions by contrasting the selected answer's explanation with the correct
              answer and its explanation whenever those fields are available.

            Coaching rules:
            - Prioritize the 2 or 3 gaps with the greatest impact instead of giving generic advice.
            - Each recommendation must name the topic, the next action, and a concrete study method.
            - Exercises must be specific enough to start immediately and should progress from recall to
              application. Do not reveal an answer before the learner attempts the exercise.
            - Keep the tone direct, encouraging, and student-facing. Avoid repetitive praise and vague phrases.
            - In references, copy only URLs supplied under verified_external_references, exactly as provided.
              Include every supplied reference once and explain which identified gap it addresses. If the list
              is empty, return an empty references list. Never create, complete, or guess a URL.

            The markdown field must be a complete 350-650 word post-practice analysis with the headings
            "Leitura do resultado", "O que funcionou", "Onde concentrar a revisão", and "Próxima sessão".
            Use concise paragraphs and bullets, do not repeat the separate references list, and do not mention
            infrastructure, events, Kafka, S3, workers, pipelines, prompts, JSON, or internal processing.
            Return only JSON matching the schema.
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
    private final String fallbackModels;
    private final AiProviderCircuitBreaker modelCircuitBreaker;
    private final int maxOutputTokens;
    private final double temperature;

    public GeminiPerformanceAnalyzer(
            @Qualifier("aiWebClient")
            WebClient aiWebClient,
            ObjectMapper objectMapper,
            @Value("${ai.model}") String model,
            @Value("${ai.fallback-models:gemini-2.5-flash,gemini-3.1-flash-lite}") String fallbackModels,
            AiProviderCircuitBreaker modelCircuitBreaker,
            @Value("${ai.max-output-tokens:8192}") int maxOutputTokens,
            @Value("${ai.temperature:0.2}") double temperature) {
        this.aiWebClient = aiWebClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.fallbackModels = fallbackModels;
        this.modelCircuitBreaker = modelCircuitBreaker;
        this.maxOutputTokens = maxOutputTokens;
        this.temperature = temperature;
    }

    public Mono<GeneratedAnalysis> analyze(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences) {
        return analyzeWithCandidates(event, webReferences, modelCandidates(), 0, null);
    }

    private Mono<GeneratedAnalysis> analyzeWithCandidates(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences,
            List<String> candidates,
            int candidateIndex,
            Throwable lastUnavailableError) {
        if (candidateIndex >= candidates.size()) {
            return Mono.error(lastUnavailableError == null
                    ? new IllegalStateException("No Gemini model is configured for performance analysis")
                    : lastUnavailableError);
        }
        String requestedModel = candidates.get(candidateIndex);
        if (!modelCircuitBreaker.allowsRequest(requestedModel)) {
            LOGGER.warn("Gemini model {} is temporarily bypassed because its circuit is open", requestedModel);
            return analyzeWithCandidates(event, webReferences, candidates, candidateIndex + 1, lastUnavailableError);
        }
        return analyzeWithModel(event, webReferences, requestedModel)
                .doOnSuccess(ignored -> modelCircuitBreaker.recordSuccess(requestedModel))
                .onErrorResume(throwable -> {
                    if (!isModelUnavailable(throwable)) {
                        return Mono.error(throwable);
                    }
                    modelCircuitBreaker.recordUnavailable(requestedModel);
                    LOGGER.warn(
                            "Gemini model {} is unavailable; opening its circuit and trying the next configured model",
                            requestedModel);
                    return analyzeWithCandidates(event, webReferences, candidates, candidateIndex + 1, throwable);
                });
    }

    private Mono<GeneratedAnalysis> analyzeWithModel(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences,
            String requestedModel) {
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                GeminiContent.system(INSTRUCTION),
                List.of(GeminiContent.user(buildPrompt(event, webReferences))),
                new GeminiGenerationConfig("application/json", RESPONSE_SCHEMA, temperature, maxOutputTokens));
        return aiWebClient.post()
                .uri("/models/{model}:generateContent", requestedModel)
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
                        .maxBackoff(Duration.ofSeconds(10))
                        .jitter(0.5)
                        .filter(GeminiPerformanceAnalyzer::isRetryableGeminiError)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .map(response -> new GeneratedAnalysis(
                        toAnalysisResult(response, webReferences),
                        "gemini",
                        requestedModel));
    }

    private List<String> modelCandidates() {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, model);
        for (String fallbackModel : fallbackModels.split(",")) {
            addCandidate(candidates, fallbackModel);
        }
        return new ArrayList<>(candidates);
    }

    private void addCandidate(LinkedHashSet<String> candidates, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            candidates.add(candidate.trim());
        }
    }

    String systemInstruction() {
        return INSTRUCTION;
    }

    String buildPrompt(StudyPerformanceAnalysisRequestedEvent event, List<Reference> webReferences) {
        try {
            return """
                    <performance_snapshot>
                    %s
                    </performance_snapshot>

                    <verified_external_references>
                    %s
                    </verified_external_references>
                    """.formatted(
                    objectMapper.writeValueAsString(event),
                    objectMapper.writeValueAsString(webReferences == null ? List.of() : webReferences));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not build performance analysis prompt", exception);
        }
    }

    PerformanceAnalysisResult toAnalysisResult(
            GeminiGenerateContentResponse response,
            List<Reference> verifiedReferences) {
        if ("MAX_TOKENS".equals(response.firstFinishReason())) {
            throw new IllegalArgumentException("Gemini response was truncated");
        }
        return parseAnalysisContent(response.firstText(), verifiedReferences);
    }

    PerformanceAnalysisResult parseAnalysisContent(String content, List<Reference> verifiedReferences) {
        try {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("AI provider returned no analysis");
            }
            PerformanceAnalysisResult result = objectMapper.readValue(stripMarkdownFence(content), PerformanceAnalysisResult.class);
            validate(result);
            return withVerifiedReferences(result, verifiedReferences);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid performance analysis response", exception);
        }
    }

    private String stripMarkdownFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, closingFence).trim();
    }

    private PerformanceAnalysisResult withVerifiedReferences(
            PerformanceAnalysisResult result,
            List<Reference> verifiedReferences) {
        if (verifiedReferences == null || verifiedReferences.isEmpty()) {
            return copyWithReferences(result, List.of());
        }

        Map<String, Reference> generatedByUrl = new LinkedHashMap<>();
        result.references().stream()
                .filter(Objects::nonNull)
                .forEach(reference -> ReferencePolicy.normalizeUrl(reference.url())
                        .ifPresent(url -> generatedByUrl.putIfAbsent(url, reference)));
        List<Reference> safeReferences = verifiedReferences.stream()
                .filter(Objects::nonNull)
                .map(reference -> ReferencePolicy.normalizeUrl(reference.url())
                        .map(url -> new Reference(reference.title(), url, reference.justification()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(reference -> mergeReference(reference, generatedByUrl.get(reference.url())))
                .toList();
        return copyWithReferences(result, safeReferences);
    }

    private Reference mergeReference(Reference verified, Reference generated) {
        if (generated == null || isBlank(generated.justification())) {
            return verified;
        }
        return new Reference(verified.title(), verified.url(), generated.justification().trim());
    }

    private PerformanceAnalysisResult copyWithReferences(
            PerformanceAnalysisResult result,
            List<Reference> references) {
        return new PerformanceAnalysisResult(
                result.markdown(),
                result.summary(),
                result.strengths(),
                result.gaps(),
                result.evolution(),
                result.recommendations(),
                result.exercises(),
                references);
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

    private static boolean isModelUnavailable(Throwable throwable) {
        return throwable instanceof GeminiHttpException exception && exception.isTransient();
    }

    private static class GeminiHttpException extends RuntimeException {

        private final int statusCode;

        GeminiHttpException(int statusCode, String responseBody) {
            super("Gemini returned HTTP " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
        }

        boolean isTransient() {
            return statusCode == 429
                    || statusCode == 500
                    || statusCode == 502
                    || statusCode == 503
                    || statusCode == 504;
        }
    }
}

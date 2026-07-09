package com.learnia.performanceanalyzer.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.AttemptAnswerSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.AttemptSnapshot;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent.QuestionSnapshot;
import com.learnia.performanceanalyzer.model.gemini.GeminiInteractionContentBlock;
import com.learnia.performanceanalyzer.model.gemini.GeminiInteractionRequest;
import com.learnia.performanceanalyzer.model.gemini.GeminiInteractionResponse;
import com.learnia.performanceanalyzer.model.gemini.GeminiInteractionStep;
import com.learnia.performanceanalyzer.model.gemini.GeminiInteractionTool;
import com.learnia.performanceanalyzer.model.gemini.GeminiUrlCitation;

import reactor.util.retry.Retry;

@Service
@ConditionalOnProperty(name = "web-search.enabled", havingValue = "true")
public class GeminiGoogleSearchService implements WebSearchService {

    private static final int MAX_REFERENCES = 5;
    private static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(45);

    private final WebClient aiWebClient;
    private final String model;

    public GeminiGoogleSearchService(
            WebClient aiWebClient,
            @Value("${ai.model}") String model) {
        this.aiWebClient = aiWebClient;
        this.model = model;
    }

    @Override
    public List<Reference> search(StudyPerformanceAnalysisRequestedEvent event) {
        GeminiInteractionRequest request = new GeminiInteractionRequest(
                model,
                buildSearchPrompt(event),
                List.of(GeminiInteractionTool.googleSearch()));
        return aiWebClient.post()
                .uri("/interactions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("No response body")
                                .map(body -> new GeminiWebSearchException(response.statusCode(), body)))
                .bodyToMono(GeminiInteractionResponse.class)
                .timeout(SEARCH_TIMEOUT)
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(6))
                        .filter(GeminiGoogleSearchService::isRetryableSearchError))
                .map(this::toReferences)
                .onErrorReturn(List.of())
                .block();
    }

    private String buildSearchPrompt(StudyPerformanceAnalysisRequestedEvent event) {
        List<String> reviewTargets = reviewTargets(event);
        String studyLanguage = event.problemSet() == null ? "pt-BR" : event.problemSet().studyLanguage();
        String fileName = event.problemSet() == null ? "material de estudo" : event.problemSet().originalFileName();
        return """
                Find current, reliable, student-friendly web references to help revise this practice.
                Study language: %s.
                Material: %s.
                Priority review targets: %s.
                Prefer official documentation, educational institutions, recognized publications, or stable learning resources.
                Return a concise answer with citations for the best sources. Do not mention implementation details.
                """.formatted(
                studyLanguage,
                fileName,
                reviewTargets.isEmpty() ? "general review of the practiced subjects" : String.join(", ", reviewTargets));
    }

    private List<String> reviewTargets(StudyPerformanceAnalysisRequestedEvent event) {
        if (event.problemSet() == null || event.problemSet().questions() == null || event.attempts() == null) {
            return List.of();
        }
        Map<java.util.UUID, QuestionSnapshot> questionsById = new LinkedHashMap<>();
        event.problemSet().questions().forEach(question -> questionsById.put(question.id(), question));
        Map<String, Integer> missesByTarget = new LinkedHashMap<>();
        event.attempts().stream()
                .filter(attempt -> "SUBMITTED".equals(attempt.status()))
                .flatMap(attempt -> answers(attempt).stream())
                .filter(answer -> !answer.correct())
                .forEach(answer -> {
                    QuestionSnapshot question = questionsById.get(answer.questionId());
                    String target = question == null
                            ? answer.subject() + " - " + answer.theme()
                            : question.subject() + " - " + question.theme();
                    missesByTarget.merge(target, 1, Integer::sum);
                });
        return missesByTarget.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<AttemptAnswerSnapshot> answers(AttemptSnapshot attempt) {
        return attempt.answers() == null ? List.of() : attempt.answers();
    }

    private List<Reference> toReferences(GeminiInteractionResponse response) {
        if (response == null || response.steps() == null) {
            return List.of();
        }
        Map<String, Reference> referencesByUrl = new LinkedHashMap<>();
        for (GeminiInteractionStep step : response.steps()) {
            if (!"model_output".equals(step.type()) || step.content() == null) {
                continue;
            }
            for (GeminiInteractionContentBlock content : step.content()) {
                if (!"text".equals(content.type()) || content.annotations() == null) {
                    continue;
                }
                collectReferences(content, referencesByUrl);
            }
        }
        return new ArrayList<>(referencesByUrl.values()).stream()
                .limit(MAX_REFERENCES)
                .toList();
    }

    private void collectReferences(GeminiInteractionContentBlock content, Map<String, Reference> referencesByUrl) {
        for (GeminiUrlCitation citation : content.annotations()) {
            if (!"url_citation".equals(citation.type()) || citation.url() == null || citation.url().isBlank()) {
                continue;
            }
            referencesByUrl.putIfAbsent(citation.url(), new Reference(
                    title(citation),
                    citation.url(),
                    citedText(content.text(), citation)));
        }
    }

    private String title(GeminiUrlCitation citation) {
        return citation.title() == null || citation.title().isBlank() ? citation.url() : citation.title();
    }

    private String citedText(String text, GeminiUrlCitation citation) {
        if (text == null || citation.startIndex() == null || citation.endIndex() == null) {
            return "Fonte sugerida para aprofundar a revisão.";
        }
        int start = Math.max(0, Math.min(citation.startIndex(), text.length()));
        int end = Math.max(start, Math.min(citation.endIndex(), text.length()));
        String cited = text.substring(start, end).trim();
        return cited.isBlank() ? "Fonte sugerida para aprofundar a revisão." : cited;
    }

    private static boolean isRetryableSearchError(Throwable throwable) {
        return throwable instanceof GeminiWebSearchException exception && exception.isTransient();
    }

    private static class GeminiWebSearchException extends RuntimeException {

        private final int statusCode;

        GeminiWebSearchException(HttpStatusCode statusCode, String responseBody) {
            super("Gemini web search returned HTTP " + statusCode.value() + ": " + responseBody);
            this.statusCode = statusCode.value();
        }

        boolean isTransient() {
            return statusCode == 500
                    || statusCode == 502
                    || statusCode == 503
                    || statusCode == 504;
        }
    }
}

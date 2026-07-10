package com.learnia.performanceanalyzer.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatRequest;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatRequest.Message;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatResponse;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatResponse.Annotation;
import com.learnia.performanceanalyzer.model.openrouter.OpenRouterChatResponse.UrlCitation;

import reactor.util.retry.Retry;

@Service
public class OpenRouterWebSearchService {

    private static final int MAX_REFERENCES = 12;
    private static final Duration SEARCH_TIMEOUT = Duration.ofSeconds(60);

    private final WebClient openRouterWebClient;
    private final GeminiGoogleSearchService searchPromptSupport;
    private final String apiKey;
    private final String model;

    public OpenRouterWebSearchService(
            @Qualifier("openRouterWebClient") WebClient openRouterWebClient,
            GeminiGoogleSearchService searchPromptSupport,
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.search-model:${openrouter.model:openai/gpt-4o}}") String model) {
        this.openRouterWebClient = openRouterWebClient;
        this.searchPromptSupport = searchPromptSupport;
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<Reference> search(StudyPerformanceAnalysisRequestedEvent event) {
        String basePrompt = searchPromptSupport.buildSearchPrompt(event);
        String topics = searchPromptSupport.reviewTopicNames(event);
        String language = event.problemSet() == null || event.problemSet().studyLanguage() == null
                ? "pt-BR"
                : event.problemSet().studyLanguage();
        List<Reference> references = new ArrayList<>(requestReferences(basePrompt));
        supplementIfMissing(references, this::isVideo, 3, """
                Search exclusively for 5 direct video lesson URLs about these study topics: %s.
                Search primarily in %s. Prefer YouTube videos from
                established educators, universities, or Khan Academy. Do not return channel pages, playlists, articles,
                PDFs, or home pages. Cite every video with its exact watch URL.
                """.formatted(topics, language));
        supplementIfMissing(references, this::isForum, 2, """
                Search exclusively for 5 direct URLs to substantive forum or community discussion threads about these
                study topics: %s. Search primarily in %s. Prefer threads with explanations and worked examples. Do not return forum home pages, search
                pages, articles, PDFs, or generic category pages. Cite every thread with its exact URL.
                """.formatted(topics, language));
        return diverseReferences(deduplicated(references));
    }

    private List<Reference> requestReferences(String prompt) {
        OpenRouterChatRequest request = new OpenRouterChatRequest(
                model,
                List.of(new Message("user", prompt)),
                null,
                0.1,
                2_500,
                List.of(OpenRouterChatRequest.Tool.webSearch()));
        return openRouterWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("No response body")
                                .map(body -> new OpenRouterSearchException(response.statusCode().value(), body)))
                .bodyToMono(OpenRouterChatResponse.class)
                .timeout(SEARCH_TIMEOUT)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(8))
                        .jitter(0.5)
                        .filter(OpenRouterWebSearchService::isUnavailable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .map(this::toReferences)
                .block();
    }

    private void supplementIfMissing(
            List<Reference> references,
            Predicate<Reference> predicate,
            int expected,
            String prompt) {
        long current = references.stream().filter(predicate).count();
        if (current >= expected) {
            return;
        }
        try {
            references.addAll(requestReferences(prompt));
        } catch (RuntimeException ignored) {
            // The main result remains useful when an optional category-specific pass fails.
        }
    }

    private List<Reference> deduplicated(List<Reference> references) {
        Map<String, Reference> referencesByUrl = new LinkedHashMap<>();
        references.forEach(reference -> referencesByUrl.putIfAbsent(reference.url(), reference));
        return List.copyOf(referencesByUrl.values());
    }

    List<Reference> toReferences(OpenRouterChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return List.of();
        }
        OpenRouterChatResponse.Message message = response.choices().getFirst().message();
        if (message == null || message.annotations() == null) {
            return List.of();
        }
        Map<String, Reference> referencesByUrl = new LinkedHashMap<>();
        for (Annotation annotation : message.annotations()) {
            if (annotation == null || !"url_citation".equals(annotation.type())) {
                continue;
            }
            UrlCitation citation = annotation.urlCitation();
            if (citation == null || citation.url() == null) {
                continue;
            }
            String url = ReferencePolicy.normalizeUrl(citation.url()).orElse(null);
            if (url == null) {
                continue;
            }
            referencesByUrl.putIfAbsent(url, new Reference(
                    textOr(citation.title(), url, 180),
                    url,
                    textOr(citation.content(), "Fonte selecionada para aprofundar esta revisão.", 360)));
        }
        return List.copyOf(referencesByUrl.values());
    }

    private static String textOr(String value, String fallback, int maxLength) {
        String text = value == null || value.isBlank() ? fallback : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private List<Reference> diverseReferences(List<Reference> references) {
        Map<String, Reference> selected = new LinkedHashMap<>();
        addMatching(selected, references, this::isVideo, 3);
        addMatching(selected, references, this::isForum, 2);
        addMatching(selected, references, this::isAcademic, 1);
        for (Reference reference : references) {
            selected.putIfAbsent(reference.url(), reference);
            if (selected.size() == MAX_REFERENCES) {
                break;
            }
        }
        return List.copyOf(selected.values());
    }

    private void addMatching(
            Map<String, Reference> selected,
            List<Reference> references,
            Predicate<Reference> predicate,
            int limit) {
        int added = 0;
        for (Reference reference : references) {
            if (!predicate.test(reference) || selected.containsKey(reference.url())) {
                continue;
            }
            selected.put(reference.url(), reference);
            if (++added == limit) {
                return;
            }
        }
    }

    private boolean isVideo(Reference reference) {
        String value = searchable(reference);
        return value.contains("youtube.com")
                || value.contains("youtu.be")
                || value.contains("vimeo.com")
                || value.contains("dailymotion.com")
                || value.contains("khanacademy.org") && (value.contains("/v/") || value.contains("vídeo"));
    }

    private boolean isForum(Reference reference) {
        String value = searchable(reference);
        return value.contains("reddit.com")
                || value.contains("stackexchange.com")
                || value.contains("stackoverflow.com")
                || value.contains("forumeiros.com")
                || value.contains("/forum")
                || value.contains("fórum");
    }

    private boolean isAcademic(Reference reference) {
        String value = searchable(reference);
        return value.contains("arxiv.org")
                || value.contains("doi.org")
                || value.contains("scielo")
                || value.contains("pubmed")
                || value.contains(".pdf");
    }

    private String searchable(Reference reference) {
        return (reference.title() + " " + reference.url()).toLowerCase(java.util.Locale.ROOT);
    }

    static boolean isUnavailable(Throwable throwable) {
        return throwable instanceof OpenRouterSearchException exception && exception.isTransient();
    }

    private static class OpenRouterSearchException extends RuntimeException {

        private final int statusCode;

        OpenRouterSearchException(int statusCode, String responseBody) {
            super("OpenRouter web search returned HTTP " + statusCode + ": " + responseBody);
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

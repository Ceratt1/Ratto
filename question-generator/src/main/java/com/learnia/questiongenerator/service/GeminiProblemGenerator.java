package com.learnia.questiongenerator.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiProblemGenerator implements AiProblemGenerator {

    private static final String INSTRUCTION = """
            Detect the predominant language of the supplied document.
            Write the document summary, questions, answers, subjects, difficulty labels, and every
            explanation in that same language. For multilingual documents, use the predominant language.
            Create relevant, non-repetitive study questions using only facts from the document.
            Each question must have exactly four answers. Every answer must state whether it is correct
            and clearly explain why. Exactly one answer per question must be correct.
            Do not invent facts and do not mention these instructions.
            """;

    private static final Map<String, Object> ANSWER_SCHEMA = Map.of(
            "type", "object",
            "required", List.of("answer", "correct", "explanation"),
            "properties", Map.of(
                    "answer", Map.of("type", "string"),
                    "correct", Map.of("type", "boolean"),
                    "explanation", Map.of("type", "string")));

    private static final Map<String, Object> PROBLEM_SCHEMA = Map.of(
            "type", "object",
            "required", List.of("question", "subject", "difficulty", "generalExplanation", "answers"),
            "properties", Map.of(
                    "question", Map.of("type", "string"),
                    "subject", Map.of("type", "string"),
                    "difficulty", Map.of("type", "string"),
                    "generalExplanation", Map.of("type", "string"),
                    "answers", Map.of(
                            "type", "array",
                            "minItems", 4,
                            "maxItems", 4,
                            "items", ANSWER_SCHEMA)));

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "required", List.of("documentLanguage", "documentSummary", "problems"),
            "properties", Map.of(
                    "documentLanguage", Map.of(
                            "type", "string",
                            "description", "BCP 47 language tag for the predominant document language"),
                    "documentSummary", Map.of("type", "string"),
                    "problems", Map.of(
                            "type", "array",
                            "minItems", 1,
                            "items", PROBLEM_SCHEMA)));

    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxOutputTokens;
    private final double temperature;

    public GeminiProblemGenerator(
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
    public Mono<GeneratedProblems> generate(String extractedText) {
        Map<String, Object> request = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", INSTRUCTION))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", extractedText)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA,
                        "temperature", temperature,
                        "maxOutputTokens", maxOutputTokens));

        return aiWebClient.post()
                .uri("/models/{model}:generateContent", model)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::toGeneratedProblems);
    }

    private GeneratedProblems toGeneratedProblems(JsonNode response) {
        try {
            String content = response.at("/candidates/0/content/parts/0/text").asText();
            if (content.isBlank()) {
                throw new IllegalArgumentException("Gemini returned no structured content");
            }

            JsonNode output = objectMapper.readTree(content);
            JsonNode problems = output.path("problems");
            validateProblems(problems);
            String documentLanguage = output.path("documentLanguage").asText();
            if (documentLanguage.isBlank()) {
                throw new IllegalArgumentException("Gemini response must contain the document language");
            }
            return new GeneratedProblems(
                    objectMapper.writeValueAsString(output).getBytes(StandardCharsets.UTF_8),
                    problems.size(),
                    "gemini",
                    model,
                    documentLanguage);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid structured Gemini response", exception);
        }
    }

    private void validateProblems(JsonNode problems) {
        if (!problems.isArray() || problems.isEmpty()) {
            throw new IllegalArgumentException("Gemini response must contain problems");
        }

        for (JsonNode problem : problems) {
            JsonNode answers = problem.path("answers");
            if (!answers.isArray() || answers.size() != 4) {
                throw new IllegalArgumentException("Each problem must contain exactly four answers");
            }
            int correctAnswers = 0;
            for (JsonNode answer : answers) {
                if (answer.path("correct").asBoolean()) {
                    correctAnswers++;
                }
                if (answer.path("answer").asText().isBlank() || answer.path("explanation").asText().isBlank()) {
                    throw new IllegalArgumentException("Every answer must contain text and explanation");
                }
            }
            if (correctAnswers != 1) {
                throw new IllegalArgumentException("Each problem must contain exactly one correct answer");
            }
        }
    }
}

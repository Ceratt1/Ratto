package com.learnia.questiongenerator.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.learnia.models.study.StudyAnswer;
import com.learnia.models.study.StudyProblem;
import com.learnia.models.study.StudyProblemSet;
import com.learnia.questiongenerator.model.gemini.GeminiContent;
import com.learnia.questiongenerator.model.gemini.GeminiGenerateContentRequest;
import com.learnia.questiongenerator.model.gemini.GeminiGenerateContentResponse;
import com.learnia.questiongenerator.model.gemini.GeminiGenerationConfig;
import com.learnia.questiongenerator.model.gemini.GeminiJsonSchema;
import com.learnia.questiongenerator.model.GeneratedProblems;

import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiProblemGenerator implements AiProblemGenerator {

    private static final int MINIMUM_PROBLEM_COUNT = 5;
    private static final int MAXIMUM_PROBLEM_COUNT = 5;
    private static final int ANSWERS_PER_PROBLEM = 4;
    private static final int CORRECT_ANSWERS_PER_PROBLEM = 1;
    private static final int INCORRECT_ANSWERS_PER_PROBLEM = ANSWERS_PER_PROBLEM - CORRECT_ANSWERS_PER_PROBLEM;

    private static final String INSTRUCTION = """
            Detect the predominant language of the supplied document.
            Write the document summary, questions, answers, subjects, difficulty labels, and every
            explanation in that same language. For multilingual documents, use the predominant language.
            Create relevant, non-repetitive study questions using only facts from the document.
            Generate exactly 5 questions, prioritizing the document's most important and distinct topics.
            For each question, classify its broad subject and a specific theme suitable for tracking
            learning performance, such as Biology as the subject and Cells as the theme.
            Each question must have exactly four answers: three incorrect alternatives and one correct
            alternative. Every answer must state whether it is correct and clearly explain why.
            Do not invent facts and do not mention these instructions.
            """;

    private static final GeminiJsonSchema ANSWER_SCHEMA = GeminiJsonSchema.object(
            List.of("answer", "correct", "explanation"),
            Map.of(
                    "answer", GeminiJsonSchema.string(),
                    "correct", GeminiJsonSchema.bool(),
                    "explanation", GeminiJsonSchema.string()));

    private static final GeminiJsonSchema PROBLEM_SCHEMA = GeminiJsonSchema.object(
            List.of("question", "subject", "theme", "difficulty", "generalExplanation", "answers"),
            Map.of(
                    "question", GeminiJsonSchema.string(),
                    "subject", GeminiJsonSchema.string(),
                    "theme", GeminiJsonSchema.string(),
                    "difficulty", GeminiJsonSchema.string(),
                    "generalExplanation", GeminiJsonSchema.string(),
                    "answers", GeminiJsonSchema.array(ANSWER_SCHEMA, 4, 4)));

    private static final GeminiJsonSchema RESPONSE_SCHEMA = GeminiJsonSchema.object(
            List.of("documentLanguage", "documentSummary", "problems"),
            Map.of(
                    "documentLanguage", GeminiJsonSchema.string(
                            "BCP 47 language tag for the predominant document language"),
                    "documentSummary", GeminiJsonSchema.string(),
                    "problems", GeminiJsonSchema.array(PROBLEM_SCHEMA, MINIMUM_PROBLEM_COUNT, MAXIMUM_PROBLEM_COUNT)));

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
        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                GeminiContent.system(INSTRUCTION),
                List.of(GeminiContent.user(extractedText)),
                new GeminiGenerationConfig(
                        "application/json",
                        RESPONSE_SCHEMA,
                        temperature,
                        maxOutputTokens));

        return aiWebClient.post()
                .uri("/models/{model}:generateContent", model)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("No response body")
                                .map(body -> new IllegalArgumentException(
                                        "Gemini returned HTTP " + response.statusCode().value() + ": " + body)))
                .bodyToMono(GeminiGenerateContentResponse.class)
                .map(this::toGeneratedProblems);
    }

    private GeneratedProblems toGeneratedProblems(GeminiGenerateContentResponse response) {
        try {
            if ("MAX_TOKENS".equals(response.firstFinishReason())) {
                throw new IllegalArgumentException("Gemini response was truncated after reaching the output token limit");
            }

            String content = response.firstText();
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Gemini returned no structured content");
            }

            StudyProblemSet problemSet = objectMapper.readValue(content, StudyProblemSet.class);
            validate(problemSet);
            return new GeneratedProblems(problemSet, "gemini", model);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid structured Gemini response", exception);
        }
    }

    private void validate(StudyProblemSet problemSet) {
        if (isBlank(problemSet.documentLanguage())) {
            throw new IllegalArgumentException("Gemini response must contain the document language");
        }
        if (isBlank(problemSet.documentSummary())) {
            throw new IllegalArgumentException("Gemini response must contain the document summary");
        }
        if (problemSet.problems() == null
                || problemSet.problems().size() < MINIMUM_PROBLEM_COUNT
                || problemSet.problems().size() > MAXIMUM_PROBLEM_COUNT) {
            throw new IllegalArgumentException("Gemini response must contain exactly five problems");
        }

        for (StudyProblem problem : problemSet.problems()) {
            validateProblem(problem);
        }
    }

    private void validateProblem(StudyProblem problem) {
        if (isBlank(problem.question())
                || isBlank(problem.subject())
                || isBlank(problem.theme())
                || isBlank(problem.difficulty())
                || isBlank(problem.generalExplanation())) {
            throw new IllegalArgumentException("Every problem must contain all descriptive fields");
        }
        if (problem.answers() == null || problem.answers().size() != ANSWERS_PER_PROBLEM) {
            throw new IllegalArgumentException("Each problem must contain exactly four answers");
        }

        int correctAnswers = 0;
        for (StudyAnswer answer : problem.answers()) {
            if (answer.correct()) {
                correctAnswers++;
            }
            if (isBlank(answer.answer()) || isBlank(answer.explanation())) {
                throw new IllegalArgumentException("Every answer must contain text and explanation");
            }
        }
        int incorrectAnswers = problem.answers().size() - correctAnswers;
        if (correctAnswers != CORRECT_ANSWERS_PER_PROBLEM || incorrectAnswers != INCORRECT_ANSWERS_PER_PROBLEM) {
            throw new IllegalArgumentException(
                    "Each problem must contain exactly one correct answer and three incorrect answers");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

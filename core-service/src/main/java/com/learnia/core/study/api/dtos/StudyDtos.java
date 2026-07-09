package com.learnia.core.study.api.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class StudyDtos {

    private StudyDtos() {
    }

    public record ProblemSetSummaryResponse(
            UUID id,
            UUID fileUuid,
            UUID workspaceId,
            String originalFileName,
            String description,
            String documentLanguage,
            String studyLanguage,
            int questionCount,
            OffsetDateTime createdAt) {
    }

    public record ProblemSetDetailResponse(
            UUID id,
            UUID fileUuid,
            UUID workspaceId,
            String originalFileName,
            String description,
            String documentLanguage,
            String studyLanguage,
            String documentSummary,
            List<QuestionResponse> questions,
            OffsetDateTime createdAt) {
    }

    public record WorkspacePerformanceResponse(
            int totalProblemSets,
            int totalAttempts,
            int answeredQuestions,
            int correctAnswers,
            int wrongAnswers,
            BigDecimal scorePercent,
            List<ProblemSetPerformanceSummaryResponse> problemSets) {
    }

    public record ProblemSetPerformanceSummaryResponse(
            UUID problemSetId,
            String fileName,
            int attemptCount,
            int questionCount,
            int answeredCount,
            int correctCount,
            int wrongCount,
            BigDecimal scorePercent,
            List<AttemptPerformanceSummaryResponse> attempts,
            List<String> subjects) {
    }

    public record AttemptPerformanceSummaryResponse(
            UUID attemptId,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            int answeredCount,
            int correctCount,
            int wrongCount,
            BigDecimal scorePercent,
            String status) {
    }

    public record ProblemSetPerformanceResponse(
            UUID problemSetId,
            String fileName,
            String description,
            String documentSummary,
            int attemptCount,
            int questionCount,
            int answeredCount,
            int correctCount,
            int wrongCount,
            BigDecimal scorePercent,
            List<PerformanceBreakdownResponse> subjects,
            List<PerformanceBreakdownResponse> themes,
            List<QuestionPerformanceResponse> questions) {
    }

    public record PerformanceBreakdownResponse(
            String name,
            int answeredCount,
            int correctCount,
            int wrongCount,
            BigDecimal scorePercent) {
    }

    public record QuestionPerformanceResponse(
            UUID questionId,
            String question,
            String subject,
            String theme,
            String difficulty,
            int correctCount,
            int wrongCount,
            UUID lastSelectedAnswerId,
            String lastSelectedAnswer,
            UUID correctAnswerId,
            String correctAnswer,
            String explanation) {
    }

    public record QuestionResponse(
            UUID id,
            int position,
            String question,
            String subject,
            String theme,
            String difficulty,
            String generalExplanation,
            List<AnswerResponse> answers) {
    }

    public record AnswerResponse(
            UUID id,
            int position,
            String answer,
            String explanation,
            Boolean correct) {
    }

    public record MoveProblemSetRequest(UUID workspaceId) {
    }

    public record SubmitAttemptRequest(@NotEmpty List<@Valid SubmitAttemptAnswerRequest> answers) {
    }

    public record SubmitAttemptAnswerRequest(@NotNull UUID questionId, @NotNull UUID answerId) {
    }

    public record AnswerAttemptQuestionRequest(@NotNull UUID questionId, @NotNull UUID answerId) {
    }

    public record AnswerAttemptQuestionResponse(
            UUID questionId,
            UUID selectedAnswerId,
            UUID correctAnswerId,
            boolean correct,
            String selectedExplanation,
            String generalExplanation,
            int answeredCount,
            int correctCount,
            int totalQuestions,
            BigDecimal score,
            String status) {
    }

    public record AttemptResponse(
            UUID id,
            UUID problemSetId,
            String status,
            BigDecimal score,
            Integer correctCount,
            int totalQuestions,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            List<AttemptAnswerResponse> answers) {
    }

    public record AttemptAnswerResponse(
            UUID questionId,
            UUID selectedAnswerId,
            boolean correct) {
    }

    public record PerformanceAnalysisResponse(
            UUID analysisRequestId,
            String status,
            String summary,
            String markdown,
            List<String> strengths,
            List<String> gaps,
            List<String> evolution,
            List<String> recommendations,
            List<String> exercises,
            List<PerformanceAnalysisReferenceResponse> references,
            String failureReason,
            OffsetDateTime requestedAt,
            OffsetDateTime generatedAt,
            int version) {
    }

    public record PerformanceAnalysisReferenceResponse(
            String title,
            String url,
            String justification) {
    }
}

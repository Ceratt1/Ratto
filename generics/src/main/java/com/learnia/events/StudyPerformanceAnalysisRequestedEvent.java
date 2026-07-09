package com.learnia.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudyPerformanceAnalysisRequestedEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID problemSetId,
        UUID attemptId,
        UUID analysisRequestId,
        ProblemSetSnapshot problemSet,
        List<AttemptSnapshot> attempts,
        PerformanceAnalysisSnapshot previousAnalysis) {

    public record ProblemSetSnapshot(
            UUID id,
            UUID fileUuid,
            String originalFileName,
            String description,
            String documentLanguage,
            String studyLanguage,
            String documentSummary,
            List<QuestionSnapshot> questions) {
    }

    public record QuestionSnapshot(
            UUID id,
            int position,
            String question,
            String subject,
            String theme,
            String difficulty,
            String generalExplanation,
            List<AnswerSnapshot> answers) {
    }

    public record AnswerSnapshot(
            UUID id,
            int position,
            String answer,
            String explanation,
            boolean correct) {
    }

    public record AttemptSnapshot(
            UUID id,
            String status,
            BigDecimal score,
            Integer correctCount,
            int totalQuestions,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            List<AttemptAnswerSnapshot> answers) {
    }

    public record AttemptAnswerSnapshot(
            UUID questionId,
            UUID selectedAnswerId,
            boolean correct,
            String subject,
            String theme,
            String difficulty) {
    }

    public record PerformanceAnalysisSnapshot(
            UUID analysisRequestId,
            String status,
            String summary,
            String markdown,
            OffsetDateTime generatedAt) {
    }
}

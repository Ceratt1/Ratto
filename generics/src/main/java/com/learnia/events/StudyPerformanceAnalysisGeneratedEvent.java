package com.learnia.events;

import java.util.List;
import java.util.UUID;

public record StudyPerformanceAnalysisGeneratedEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID problemSetId,
        UUID attemptId,
        UUID analysisRequestId,
        String aiProvider,
        String aiModel,
        PerformanceAnalysisResult analysis) {

    public record PerformanceAnalysisResult(
            String markdown,
            String summary,
            List<String> strengths,
            List<String> gaps,
            List<String> evolution,
            List<String> recommendations,
            List<String> exercises,
            List<Reference> references) {
    }

    public record Reference(
            String title,
            String url,
            String justification) {
    }
}

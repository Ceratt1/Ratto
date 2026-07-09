package com.learnia.events;

import java.util.UUID;

public record StudyPerformanceAnalysisFailedEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID problemSetId,
        UUID attemptId,
        UUID analysisRequestId,
        String stage,
        String reason) {
}

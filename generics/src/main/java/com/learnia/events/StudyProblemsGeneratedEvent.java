package com.learnia.events;

import java.util.UUID;

public record StudyProblemsGeneratedEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID uuidRequest,
        UUID fileUuid,
        String extractedTextS3Path,
        String studyProblemsS3Path,
        String aiProvider,
        String aiModel,
        String documentLanguage,
        int problemCount,
        long outputBytes) {
}

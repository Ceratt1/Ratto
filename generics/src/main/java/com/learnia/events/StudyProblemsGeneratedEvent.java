package com.learnia.events;

import java.util.UUID;

public record StudyProblemsGeneratedEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID uuidRequest,
        UUID fileUuid,
        UUID workspaceId,
        String originalFileName,
        String extractedTextS3Path,
        String studyProblemsS3Path,
        String description,
        String aiProvider,
        String aiModel,
        String documentLanguage,
        String studyLanguage,
        int problemCount,
        long outputBytes) implements PdfIngestionEvent {
}

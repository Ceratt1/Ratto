package com.learnia.events;

import java.util.UUID;

public record PdfProcessingEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID uuidRequest,
        UUID fileUuid,
        String originalFileName,
        String pdfS3Path,
        String extractedTextS3Path,
        String description,
        String createdAt) implements PdfIngestionEvent {
}

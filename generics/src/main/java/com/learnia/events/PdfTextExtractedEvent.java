package com.learnia.events;

import java.util.UUID;

public record PdfTextExtractedEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID uuidRequest,
        UUID fileUuid,
        String originalFileName,
        String pdfS3Path,
        String extractedTextS3Path,
        long extractedTextBytes) {
}

package com.learnia.events;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record PdfIngestionErrorEvent(
        EventMetadata metadata,
        UUID uuidUser,
        UUID uuidRequest,
        UUID fileUuid,
        String failedService,
        String failedPhase,
        String reason,
        String exceptionType,
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        String failedAt) {

    public static PdfIngestionErrorEvent from(
            PdfIngestionEvent source,
            String failedService,
            String failedPhase,
            String sourceTopic,
            int sourcePartition,
            long sourceOffset,
            Exception exception) {
        Throwable rootCause = rootCause(exception);
        EventMetadata sourceMetadata = source != null ? source.metadata() : null;
        String occurredAt = OffsetDateTime.now(ZoneOffset.UTC).toString();

        return new PdfIngestionErrorEvent(
                new EventMetadata(
                        UUID.randomUUID(),
                        sourceMetadata != null && sourceMetadata.correlationId() != null
                                ? sourceMetadata.correlationId()
                                : source != null ? source.uuidRequest() : null,
                        sourceMetadata != null ? sourceMetadata.eventId() : null,
                        EventTypes.PDF_INGESTION_FAILED,
                        failedService,
                        "1",
                        occurredAt),
                source != null ? source.uuidUser() : null,
                source != null ? source.uuidRequest() : null,
                source != null ? source.fileUuid() : null,
                failedService,
                failedPhase,
                message(rootCause),
                rootCause.getClass().getName(),
                sourceTopic,
                sourcePartition,
                sourceOffset,
                occurredAt);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String message(Throwable throwable) {
        return throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}

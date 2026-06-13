package com.learnia.consumer.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.learnia.events.EventIdFactory;
import com.learnia.events.EventMetadata;
import com.learnia.events.EventTypes;
import com.learnia.events.PdfProcessingEvent;
import com.learnia.events.PdfTextExtractedEvent;
import com.learnia.tools.aws.service.S3StorageService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class PdfProcessingService {

    private final S3StorageService s3StorageService;
    private final PdfTextExtractor pdfTextExtractor;

    public PdfProcessingService(S3StorageService s3StorageService, PdfTextExtractor pdfTextExtractor) {
        this.s3StorageService = s3StorageService;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    public Mono<PdfTextExtractedEvent> process(PdfProcessingEvent event) {
        validatePaths(event);
        return s3StorageService.downloadFile(event.pdfS3Path())
                .publishOn(Schedulers.boundedElastic())
                .map(pdfTextExtractor::extract)
                .map(text -> text.getBytes(StandardCharsets.UTF_8))
                .flatMap(textBytes -> s3StorageService.uploadBytes(
                                event.extractedTextS3Path(),
                                textBytes,
                                "text/plain; charset=utf-8")
                        .thenReturn(toExtractedEvent(event, textBytes.length)));
    }

    private PdfTextExtractedEvent toExtractedEvent(PdfProcessingEvent event, long extractedTextBytes) {
        EventMetadata sourceMetadata = event.metadata();
        UUID correlationId = sourceMetadata != null && sourceMetadata.correlationId() != null
                ? sourceMetadata.correlationId()
                : event.uuidRequest();
        UUID causationId = sourceMetadata != null ? sourceMetadata.eventId() : null;

        return new PdfTextExtractedEvent(
                new EventMetadata(
                        EventIdFactory.forFile(EventTypes.PDF_TEXT_EXTRACTED, event.fileUuid()),
                        correlationId,
                        causationId,
                        EventTypes.PDF_TEXT_EXTRACTED,
                        "pdf-extractor",
                        "1",
                        OffsetDateTime.now(ZoneOffset.UTC).toString()),
                event.uuidUser(),
                event.uuidRequest(),
                event.fileUuid(),
                event.originalFileName(),
                event.pdfS3Path(),
                event.extractedTextS3Path(),
                extractedTextBytes);
    }

    private void validatePaths(PdfProcessingEvent event) {
        if (event.pdfS3Path() == null || !event.pdfS3Path().endsWith("/original.pdf")) {
            throw new IllegalArgumentException("Invalid PDF S3 path");
        }

        String expectedTextPath = event.pdfS3Path()
                .substring(0, event.pdfS3Path().lastIndexOf('/') + 1)
                + "extracted.txt";
        if (!expectedTextPath.equals(event.extractedTextS3Path())) {
            throw new IllegalArgumentException("Extracted text must use the same S3 folder as the PDF");
        }
    }
}

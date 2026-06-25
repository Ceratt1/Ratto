package com.learnia.questiongenerator.service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.learnia.events.EventIdFactory;
import com.learnia.events.EventMetadata;
import com.learnia.events.EventTypes;
import com.learnia.events.PdfTextExtractedEvent;
import com.learnia.events.StudyProblemsGeneratedEvent;
import com.learnia.questiongenerator.model.GeneratedProblems;
import com.learnia.tools.aws.service.S3StorageService;

import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
public class QuestionGenerationService {

    private final S3StorageService s3StorageService;
    private final AiProblemGenerator aiProblemGenerator;
    private final ObjectMapper objectMapper;

    public QuestionGenerationService(
            S3StorageService s3StorageService,
            AiProblemGenerator aiProblemGenerator,
            ObjectMapper objectMapper) {
        this.s3StorageService = s3StorageService;
        this.aiProblemGenerator = aiProblemGenerator;
        this.objectMapper = objectMapper;
    }

    public Mono<StudyProblemsGeneratedEvent> process(PdfTextExtractedEvent event) {
        String outputPath = outputPath(event);
        return s3StorageService.downloadFile(event.extractedTextS3Path())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .flatMap(text -> aiProblemGenerator.generate(text, event.description(), event.studyLanguage()))
                .flatMap(result -> {
                    byte[] output = serialize(result);
                    return s3StorageService.uploadBytes(outputPath, output, "application/json")
                            .thenReturn(toCompletedEvent(event, outputPath, output.length, result));
                });
    }

    private String outputPath(PdfTextExtractedEvent event) {
        String suffix = "/extracted.txt";
        if (event.extractedTextS3Path() == null || !event.extractedTextS3Path().endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid extracted text S3 path");
        }
        return event.extractedTextS3Path().substring(0, event.extractedTextS3Path().length() - suffix.length())
                + "/questions.json";
    }

    private StudyProblemsGeneratedEvent toCompletedEvent(
            PdfTextExtractedEvent event,
            String outputPath,
            long outputBytes,
            GeneratedProblems result) {
        EventMetadata source = event.metadata();
        UUID correlationId = source != null && source.correlationId() != null
                ? source.correlationId()
                : event.uuidRequest();
        return new StudyProblemsGeneratedEvent(
                new EventMetadata(
                        EventIdFactory.forFile(EventTypes.STUDY_PROBLEMS_GENERATED, event.fileUuid()),
                        correlationId,
                        source != null ? source.eventId() : null,
                        EventTypes.STUDY_PROBLEMS_GENERATED,
                        "question-generator",
                        "1",
                        OffsetDateTime.now(ZoneOffset.UTC).toString()),
                event.uuidUser(),
                event.uuidRequest(),
                event.fileUuid(),
                event.workspaceId(),
                event.originalFileName(),
                event.extractedTextS3Path(),
                outputPath,
                event.description(),
                result.aiProvider(),
                result.aiModel(),
                result.problemSet().documentLanguage(),
                result.problemSet().studyLanguage(),
                result.problemCount(),
                outputBytes);
    }

    private byte[] serialize(GeneratedProblems result) {
        try {
            return objectMapper.writeValueAsBytes(result.problemSet());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not serialize generated study problems", exception);
        }
    }
}

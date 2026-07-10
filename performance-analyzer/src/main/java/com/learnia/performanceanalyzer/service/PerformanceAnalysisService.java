package com.learnia.performanceanalyzer.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.learnia.events.EventIdFactory;
import com.learnia.events.EventMetadata;
import com.learnia.events.EventTopics;
import com.learnia.events.EventTypes;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.performanceanalyzer.service.AiPerformanceAnalyzer.GeneratedAnalysis;

import reactor.core.publisher.Mono;

@Service
public class PerformanceAnalysisService {

    private final WebSearchService webSearchService;
    private final AiPerformanceAnalyzer aiPerformanceAnalyzer;
    private final KafkaTemplate<String, StudyPerformanceAnalysisGeneratedEvent> generatedEventKafkaTemplate;

    public PerformanceAnalysisService(
            WebSearchService webSearchService,
            AiPerformanceAnalyzer aiPerformanceAnalyzer,
            KafkaTemplate<String, StudyPerformanceAnalysisGeneratedEvent> generatedEventKafkaTemplate) {
        this.webSearchService = webSearchService;
        this.aiPerformanceAnalyzer = aiPerformanceAnalyzer;
        this.generatedEventKafkaTemplate = generatedEventKafkaTemplate;
    }

    public Mono<Void> process(StudyPerformanceAnalysisRequestedEvent event) {
        return Mono.fromCallable(() -> webSearchService.search(event))
                .flatMap(references -> aiPerformanceAnalyzer.analyze(event, references))
                .map(result -> toGeneratedEvent(event, result))
                .doOnNext(generated -> generatedEventKafkaTemplate.send(
                        EventTopics.STUDY_PERFORMANCE_ANALYSIS_GENERATED,
                        generated.problemSetId().toString(),
                        generated).join())
                .then();
    }

    private StudyPerformanceAnalysisGeneratedEvent toGeneratedEvent(
            StudyPerformanceAnalysisRequestedEvent event,
            GeneratedAnalysis generatedAnalysis) {
        EventMetadata source = event.metadata();
        return new StudyPerformanceAnalysisGeneratedEvent(
                new EventMetadata(
                        EventIdFactory.forFile(
                                EventTypes.STUDY_PERFORMANCE_ANALYSIS_GENERATED,
                                event.analysisRequestId()),
                        source == null ? event.analysisRequestId() : source.correlationId(),
                        source == null ? null : source.eventId(),
                        EventTypes.STUDY_PERFORMANCE_ANALYSIS_GENERATED,
                        "performance-analyzer",
                        "1",
                        OffsetDateTime.now(ZoneOffset.UTC).toString()),
                event.uuidUser(),
                event.problemSetId(),
                event.attemptId(),
                event.analysisRequestId(),
                generatedAnalysis.provider(),
                generatedAnalysis.model(),
                generatedAnalysis.result());
    }
}

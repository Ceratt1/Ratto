package com.learnia.performanceanalyzer.service;

import java.util.List;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.PerformanceAnalysisResult;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;

import reactor.core.publisher.Mono;

public interface AiPerformanceAnalyzer {

    Mono<PerformanceAnalysisResult> analyze(
            StudyPerformanceAnalysisRequestedEvent event,
            List<Reference> webReferences);
}

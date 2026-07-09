package com.learnia.core.study.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.learnia.core.study.services.StudyService;
import com.learnia.events.EventTopics;
import com.learnia.events.StudyPerformanceAnalysisFailedEvent;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent;

@Component
public class StudyPerformanceAnalysisListener {

    private final StudyService studyService;

    public StudyPerformanceAnalysisListener(StudyService studyService) {
        this.studyService = studyService;
    }

    @KafkaListener(
            topics = EventTopics.STUDY_PERFORMANCE_ANALYSIS_GENERATED,
            groupId = "core-service-performance-analysis",
            containerFactory = "performanceAnalysisGeneratedKafkaListenerContainerFactory")
    public void consumeGenerated(StudyPerformanceAnalysisGeneratedEvent event) {
        studyService.applyGeneratedPerformanceAnalysis(event);
    }

    @KafkaListener(
            topics = EventTopics.STUDY_PERFORMANCE_ANALYSIS_ERRORS,
            groupId = "core-service-performance-analysis",
            containerFactory = "performanceAnalysisFailedKafkaListenerContainerFactory")
    public void consumeFailed(StudyPerformanceAnalysisFailedEvent event) {
        studyService.applyFailedPerformanceAnalysis(event);
    }
}

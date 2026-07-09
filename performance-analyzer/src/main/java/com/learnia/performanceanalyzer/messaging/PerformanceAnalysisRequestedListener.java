package com.learnia.performanceanalyzer.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.learnia.events.EventTopics;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;
import com.learnia.performanceanalyzer.service.PerformanceAnalysisService;

@Component
public class PerformanceAnalysisRequestedListener {

    private final PerformanceAnalysisService performanceAnalysisService;

    public PerformanceAnalysisRequestedListener(PerformanceAnalysisService performanceAnalysisService) {
        this.performanceAnalysisService = performanceAnalysisService;
    }

    @KafkaListener(topics = EventTopics.STUDY_PERFORMANCE_ANALYSIS_REQUESTED, groupId = "performance-analyzer")
    public void consume(StudyPerformanceAnalysisRequestedEvent event) {
        performanceAnalysisService.process(event).block();
    }
}

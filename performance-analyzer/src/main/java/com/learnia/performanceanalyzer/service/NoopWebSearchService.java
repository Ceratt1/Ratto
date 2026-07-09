package com.learnia.performanceanalyzer.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;

@Service
@ConditionalOnProperty(name = "web-search.enabled", havingValue = "false", matchIfMissing = true)
public class NoopWebSearchService implements WebSearchService {

    @Override
    public List<Reference> search(StudyPerformanceAnalysisRequestedEvent event) {
        return List.of();
    }
}

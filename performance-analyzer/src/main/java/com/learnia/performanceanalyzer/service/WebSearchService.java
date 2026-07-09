package com.learnia.performanceanalyzer.service;

import java.util.List;

import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent.Reference;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;

public interface WebSearchService {

    List<Reference> search(StudyPerformanceAnalysisRequestedEvent event);
}

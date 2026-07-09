package com.learnia.events;

public final class EventTopics {

    public static final String PDF_PROCESSING_REQUESTED = "knowledgement-topic";
    public static final String PDF_TEXT_EXTRACTED = "pdf-text-extracted-topic";
    public static final String STUDY_PROBLEMS_GENERATED = "study-problems-generated-topic";
    public static final String PDF_INGESTION_ERRORS = "pdf-ingestion-errors";
    public static final String STUDY_PERFORMANCE_ANALYSIS_REQUESTED = "study-performance-analysis-requested-topic";
    public static final String STUDY_PERFORMANCE_ANALYSIS_GENERATED = "study-performance-analysis-generated-topic";
    public static final String STUDY_PERFORMANCE_ANALYSIS_ERRORS = "study-performance-analysis-errors";

    private EventTopics() {
    }
}

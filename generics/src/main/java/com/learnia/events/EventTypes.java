package com.learnia.events;

public final class EventTypes {

    public static final String PDF_PROCESSING_REQUESTED = "PDF_PROCESSING_REQUESTED";
    public static final String PDF_TEXT_EXTRACTED = "PDF_TEXT_EXTRACTED";
    public static final String STUDY_PROBLEMS_GENERATED = "STUDY_PROBLEMS_GENERATED";
    public static final String PDF_INGESTION_FAILED = "PDF_INGESTION_FAILED";
    public static final String STUDY_PERFORMANCE_ANALYSIS_REQUESTED = "STUDY_PERFORMANCE_ANALYSIS_REQUESTED";
    public static final String STUDY_PERFORMANCE_ANALYSIS_GENERATED = "STUDY_PERFORMANCE_ANALYSIS_GENERATED";
    public static final String STUDY_PERFORMANCE_ANALYSIS_FAILED = "STUDY_PERFORMANCE_ANALYSIS_FAILED";

    private EventTypes() {
    }
}

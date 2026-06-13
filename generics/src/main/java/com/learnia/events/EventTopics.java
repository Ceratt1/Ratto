package com.learnia.events;

public final class EventTopics {

    public static final String PDF_PROCESSING_REQUESTED = "knowledgement-topic";
    public static final String PDF_PROCESSING_ERRORS = "knowledgement-topic-errors";
    public static final String PDF_TEXT_EXTRACTED = "pdf-text-extracted-topic";
    public static final String PDF_TEXT_EXTRACTED_ERRORS = "pdf-text-extracted-errors";
    public static final String STUDY_PROBLEMS_GENERATED = "study-problems-generated-topic";

    private EventTopics() {
    }
}

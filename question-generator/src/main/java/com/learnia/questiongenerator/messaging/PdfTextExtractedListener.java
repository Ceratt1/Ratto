package com.learnia.questiongenerator.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.learnia.events.EventTopics;
import com.learnia.events.PdfTextExtractedEvent;
import com.learnia.events.StudyProblemsGeneratedEvent;
import com.learnia.questiongenerator.service.QuestionGenerationService;

import reactor.core.publisher.Mono;

@Component
public class PdfTextExtractedListener {

    private final QuestionGenerationService generationService;
    private final KafkaTemplate<String, StudyProblemsGeneratedEvent> kafkaTemplate;

    public PdfTextExtractedListener(
            QuestionGenerationService generationService,
            KafkaTemplate<String, StudyProblemsGeneratedEvent> kafkaTemplate) {
        this.generationService = generationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = EventTopics.PDF_TEXT_EXTRACTED, groupId = "question-generator")
    public void consume(PdfTextExtractedEvent event) {
        generationService.process(event)
                .flatMap(completed -> Mono.fromFuture(() -> kafkaTemplate.send(
                        EventTopics.STUDY_PROBLEMS_GENERATED,
                        completed.fileUuid().toString(),
                        completed)))
                .block();
    }
}

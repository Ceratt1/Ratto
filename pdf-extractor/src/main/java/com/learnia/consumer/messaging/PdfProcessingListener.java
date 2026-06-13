package com.learnia.consumer.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.learnia.consumer.service.PdfProcessingService;
import com.learnia.events.EventTopics;
import com.learnia.events.PdfProcessingEvent;
import com.learnia.events.PdfTextExtractedEvent;

import reactor.core.publisher.Mono;

@Component
public class PdfProcessingListener {

    private final PdfProcessingService pdfProcessingService;
    private final KafkaTemplate<String, PdfTextExtractedEvent> extractedEventKafkaTemplate;

    public PdfProcessingListener(
            PdfProcessingService pdfProcessingService,
            KafkaTemplate<String, PdfTextExtractedEvent> extractedEventKafkaTemplate) {
        this.pdfProcessingService = pdfProcessingService;
        this.extractedEventKafkaTemplate = extractedEventKafkaTemplate;
    }

    @KafkaListener(topics = EventTopics.PDF_PROCESSING_REQUESTED, groupId = "pdf-extractor")
    public void consume(PdfProcessingEvent event) {
        pdfProcessingService.process(event)
                .flatMap(extractedEvent -> Mono.fromFuture(() -> extractedEventKafkaTemplate.send(
                        EventTopics.PDF_TEXT_EXTRACTED,
                        extractedEvent.fileUuid().toString(),
                        extractedEvent)))
                .block();
    }
}

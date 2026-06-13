package com.learnia.ledger.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.learnia.events.EventTopics;
import com.learnia.ledger.persistence.EventLedgerRepository;

@Component
public class EventLedgerListener {

    private final EventLedgerRepository repository;

    public EventLedgerListener(EventLedgerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @KafkaListener(
            topics = {
                    EventTopics.PDF_PROCESSING_REQUESTED,
                    EventTopics.PDF_TEXT_EXTRACTED,
                    EventTopics.STUDY_PROBLEMS_GENERATED,
                    EventTopics.PDF_INGESTION_ERRORS
            },
            groupId = "event-ledger")
    public void consume(ConsumerRecord<String, String> record) {
        repository.append(record);
    }
}

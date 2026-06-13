package com.learnia.consumer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.learnia.events.EventTopics;

@Configuration
public class KafkaTopics {

    @Bean
    public NewTopic ingestionErrorsTopic() {
        return new NewTopic(EventTopics.PDF_INGESTION_ERRORS, 2, (short) 1);
    }

    @Bean
    public NewTopic textExtractedTopic() {
        return new NewTopic(EventTopics.PDF_TEXT_EXTRACTED, 2, (short) 1);
    }
}

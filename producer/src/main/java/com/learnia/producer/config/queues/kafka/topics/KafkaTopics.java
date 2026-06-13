package com.learnia.producer.config.queues.kafka.topics;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.learnia.events.EventTopics;

@Configuration
public class KafkaTopics{
    
    @Bean
    public NewTopic knowledgementTopic() {
        return new NewTopic(EventTopics.PDF_PROCESSING_REQUESTED, 2, (short) 1);
    }
}

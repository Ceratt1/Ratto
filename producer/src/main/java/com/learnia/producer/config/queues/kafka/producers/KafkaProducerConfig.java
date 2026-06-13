package com.learnia.producer.config.queues.kafka.producers;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.learnia.events.PdfProcessingEvent;

@Configuration
public class KafkaProducerConfig {
    
    private final String BOOTSTRAP_SERVERS;

    public KafkaProducerConfig(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        this.BOOTSTRAP_SERVERS = bootstrapServers;
    }

    @Bean
    public ProducerFactory<String, PdfProcessingEvent> pdfProcessingEventFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, PdfProcessingEvent> kafkaTemplate() {
        return new KafkaTemplate<>(pdfProcessingEventFactory());
    }
}

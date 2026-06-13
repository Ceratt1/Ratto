package com.learnia.questiongenerator.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.learnia.events.EventTopics;
import com.learnia.events.PdfTextExtractedEvent;
import com.learnia.events.StudyProblemsGeneratedEvent;

@Configuration
public class KafkaConfig {

    private final String bootstrapServers;

    public KafkaConfig(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Bean
    public ConsumerFactory<String, PdfTextExtractedEvent> consumerFactory() {
        Map<String, Object> properties = baseConsumerProperties();
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PdfTextExtractedEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, Object> deadLetterProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties());
    }

    @Bean
    public KafkaTemplate<String, Object> deadLetterKafkaTemplate() {
        return new KafkaTemplate<>(deadLetterProducerFactory());
    }

    @Bean
    public ProducerFactory<String, StudyProblemsGeneratedEvent> completedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties());
    }

    @Bean
    public KafkaTemplate<String, StudyProblemsGeneratedEvent> completedEventKafkaTemplate() {
        return new KafkaTemplate<>(completedEventProducerFactory());
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterKafkaTemplate,
                (record, exception) -> new org.apache.kafka.common.TopicPartition(
                        EventTopics.PDF_TEXT_EXTRACTED_ERRORS,
                        record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(2_000L, 3L));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PdfTextExtractedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PdfTextExtractedEvent> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, PdfTextExtractedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(2);
        return factory;
    }

    private Map<String, Object> baseConsumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "question-generator");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.learnia.events");
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return properties;
    }

    private Map<String, Object> producerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return properties;
    }
}

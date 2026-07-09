package com.learnia.performanceanalyzer.config;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.clients.admin.AdminClientConfig;
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
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.learnia.events.EventIdFactory;
import com.learnia.events.EventMetadata;
import com.learnia.events.EventTopics;
import com.learnia.events.EventTypes;
import com.learnia.events.StudyPerformanceAnalysisFailedEvent;
import com.learnia.events.StudyPerformanceAnalysisGeneratedEvent;
import com.learnia.events.StudyPerformanceAnalysisRequestedEvent;

@Configuration
public class KafkaConfig {

    private final String bootstrapServers;

    public KafkaConfig(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    public ConsumerFactory<String, StudyPerformanceAnalysisRequestedEvent> consumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "performance-analyzer");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                jsonDeserializer(StudyPerformanceAnalysisRequestedEvent.class));
    }

    @Bean
    public ProducerFactory<String, StudyPerformanceAnalysisGeneratedEvent> generatedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties(), new StringSerializer(), jsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, StudyPerformanceAnalysisGeneratedEvent> generatedEventKafkaTemplate() {
        return new KafkaTemplate<>(generatedEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, StudyPerformanceAnalysisFailedEvent> failedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProperties(), new StringSerializer(), jsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, StudyPerformanceAnalysisFailedEvent> failedEventKafkaTemplate() {
        return new KafkaTemplate<>(failedEventProducerFactory());
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, StudyPerformanceAnalysisFailedEvent> failedEventKafkaTemplate) {
        return new DefaultErrorHandler((record, exception) -> {
            StudyPerformanceAnalysisRequestedEvent source = record.value() instanceof StudyPerformanceAnalysisRequestedEvent event
                    ? event
                    : null;
            StudyPerformanceAnalysisFailedEvent failedEvent = failedEvent(source, exception);
            String key = source != null && source.problemSetId() != null
                    ? source.problemSetId().toString()
                    : record.key() == null ? null : record.key().toString();
            failedEventKafkaTemplate.send(EventTopics.STUDY_PERFORMANCE_ANALYSIS_ERRORS, key, failedEvent).join();
        }, new FixedBackOff(2_000L, 3L));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StudyPerformanceAnalysisRequestedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, StudyPerformanceAnalysisRequestedEvent> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, StudyPerformanceAnalysisRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(2);
        return factory;
    }

    private Map<String, Object> producerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }

    private ObjectMapper kafkaObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private <T> JsonDeserializer<T> jsonDeserializer(Class<T> eventType) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(eventType, kafkaObjectMapper(), false);
        deserializer.addTrustedPackages("com.learnia.events");
        deserializer.setUseTypeHeaders(false);
        return deserializer;
    }

    private <T> JsonSerializer<T> jsonSerializer() {
        JsonSerializer<T> serializer = new JsonSerializer<>(kafkaObjectMapper());
        serializer.setAddTypeInfo(false);
        return serializer;
    }

    private StudyPerformanceAnalysisFailedEvent failedEvent(
            StudyPerformanceAnalysisRequestedEvent source,
            Exception exception) {
        EventMetadata sourceMetadata = source == null ? null : source.metadata();
        return new StudyPerformanceAnalysisFailedEvent(
                new EventMetadata(
                        EventIdFactory.forFile(
                                EventTypes.STUDY_PERFORMANCE_ANALYSIS_FAILED,
                                source == null ? java.util.UUID.randomUUID() : source.analysisRequestId()),
                        sourceMetadata == null ? null : sourceMetadata.correlationId(),
                        sourceMetadata == null ? null : sourceMetadata.eventId(),
                        EventTypes.STUDY_PERFORMANCE_ANALYSIS_FAILED,
                        "performance-analyzer",
                        "1",
                        OffsetDateTime.now(ZoneOffset.UTC).toString()),
                source == null ? null : source.uuidUser(),
                source == null ? null : source.problemSetId(),
                source == null ? null : source.attemptId(),
                source == null ? null : source.analysisRequestId(),
                "PERFORMANCE_ANALYSIS",
                rootCauseMessage(exception));
    }

    private String rootCauseMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}

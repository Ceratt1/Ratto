package com.learnia.core.study.config;

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

import com.learnia.events.StudyProblemsGeneratedEvent;
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
    public ConsumerFactory<String, StudyProblemsGeneratedEvent> studyProblemsConsumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "core-service-study-projection");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                jsonDeserializer(StudyProblemsGeneratedEvent.class));
    }

    @Bean
    public ConsumerFactory<String, StudyPerformanceAnalysisGeneratedEvent> performanceAnalysisGeneratedConsumerFactory() {
        Map<String, Object> properties = baseConsumerProperties("core-service-performance-analysis");
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                jsonDeserializer(StudyPerformanceAnalysisGeneratedEvent.class));
    }

    @Bean
    public ConsumerFactory<String, StudyPerformanceAnalysisFailedEvent> performanceAnalysisFailedConsumerFactory() {
        Map<String, Object> properties = baseConsumerProperties("core-service-performance-analysis");
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                jsonDeserializer(StudyPerformanceAnalysisFailedEvent.class));
    }

    @Bean
    public ProducerFactory<String, StudyPerformanceAnalysisRequestedEvent> performanceAnalysisRequestedProducerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                jsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, StudyPerformanceAnalysisRequestedEvent> performanceAnalysisRequestedKafkaTemplate() {
        return new KafkaTemplate<>(performanceAnalysisRequestedProducerFactory());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StudyProblemsGeneratedEvent> studyProblemsKafkaListenerContainerFactory(
            ConsumerFactory<String, StudyProblemsGeneratedEvent> studyProblemsConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, StudyProblemsGeneratedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(studyProblemsConsumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2_000L, 3L)));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StudyPerformanceAnalysisGeneratedEvent> performanceAnalysisGeneratedKafkaListenerContainerFactory(
            ConsumerFactory<String, StudyPerformanceAnalysisGeneratedEvent> performanceAnalysisGeneratedConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, StudyPerformanceAnalysisGeneratedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(performanceAnalysisGeneratedConsumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2_000L, 3L)));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StudyPerformanceAnalysisFailedEvent> performanceAnalysisFailedKafkaListenerContainerFactory(
            ConsumerFactory<String, StudyPerformanceAnalysisFailedEvent> performanceAnalysisFailedConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, StudyPerformanceAnalysisFailedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(performanceAnalysisFailedConsumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(2_000L, 3L)));
        return factory;
    }

    private Map<String, Object> baseConsumerProperties(String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
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
}

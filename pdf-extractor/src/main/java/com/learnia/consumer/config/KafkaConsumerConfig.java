package com.learnia.consumer.config;

import java.util.HashMap;
import java.util.Map;

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

import com.learnia.events.PdfProcessingEvent;
import com.learnia.events.EventTopics;
import com.learnia.events.PdfIngestionErrorEvent;
import com.learnia.events.PdfTextExtractedEvent;

@Configuration
public class KafkaConsumerConfig {

    private final String bootstrapServers;

    public KafkaConsumerConfig(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    public ConsumerFactory<String, PdfProcessingEvent> consumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "pdf-extractor");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.learnia.events");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PdfProcessingEvent.class.getName());
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ProducerFactory<String, PdfIngestionErrorEvent> errorEventProducerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, PdfIngestionErrorEvent> errorEventKafkaTemplate() {
        return new KafkaTemplate<>(errorEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, PdfTextExtractedEvent> extractedEventProducerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, PdfTextExtractedEvent> extractedEventKafkaTemplate() {
        return new KafkaTemplate<>(extractedEventProducerFactory());
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, PdfIngestionErrorEvent> errorEventKafkaTemplate) {
        return new DefaultErrorHandler((record, exception) -> {
            PdfProcessingEvent source = record.value() instanceof PdfProcessingEvent event ? event : null;
            PdfIngestionErrorEvent error = PdfIngestionErrorEvent.from(
                    source,
                    "pdf-extractor",
                    "PDF_TEXT_EXTRACTION",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    exception);
            String key = source != null && source.fileUuid() != null
                    ? source.fileUuid().toString()
                    : record.key() != null ? record.key().toString() : null;
            errorEventKafkaTemplate.send(EventTopics.PDF_INGESTION_ERRORS, key, error).join();
        }, new FixedBackOff(2_000L, 3L));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PdfProcessingEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PdfProcessingEvent> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, PdfProcessingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(2);
        return factory;
    }
}

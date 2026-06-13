package com.learnia.ledger.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventLedgerRepository {

    private static final String INSERT_EVENT = """
            INSERT INTO event_ledger (
                topic_name,
                partition_number,
                topic_offset,
                message_key,
                kafka_timestamp,
                payload,
                payload_sha256
            ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
            ON CONFLICT (topic_name, partition_number, topic_offset) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public EventLedgerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void append(ConsumerRecord<String, String> record) {
        jdbcTemplate.update(
                INSERT_EVENT,
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                Timestamp.from(Instant.ofEpochMilli(record.timestamp())),
                record.value(),
                sha256(record.value()));
    }

    private String sha256(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

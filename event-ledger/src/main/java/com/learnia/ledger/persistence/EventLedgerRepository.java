package com.learnia.ledger.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.learnia.events.EventTypes;
import com.learnia.ledger.api.dto.IngestionStatusResponse;

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
    private static final String FIND_LATEST_FILE_EVENT = """
            SELECT event_type, payload ->> 'reason' AS reason
            FROM event_ledger
            WHERE uuid_user = ? AND file_uuid = ?
            ORDER BY recorded_at DESC, kafka_timestamp DESC
            LIMIT 1
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

    public IngestionStatusResponse findIngestionStatus(UUID uuidUser, UUID fileUuid) {
        return jdbcTemplate.query(
                FIND_LATEST_FILE_EVENT,
                ps -> {
                    ps.setObject(1, uuidUser);
                    ps.setObject(2, fileUuid);
                },
                rs -> {
                    if (!rs.next()) {
                        return new IngestionStatusResponse(
                                fileUuid,
                                "QUEUED",
                                "O Ratto está pegando seus materiais.",
                                null);
                    }
                    String eventType = rs.getString("event_type");
                    String reason = rs.getString("reason");
                    return toStatus(fileUuid, eventType, reason);
                });
    }

    private IngestionStatusResponse toStatus(UUID fileUuid, String eventType, String reason) {
        return switch (eventType) {
            case EventTypes.PDF_PROCESSING_REQUESTED -> new IngestionStatusResponse(
                    fileUuid,
                    "READING",
                    "O Ratto está lendo seu PDF...",
                    null);
            case EventTypes.PDF_TEXT_EXTRACTED -> new IngestionStatusResponse(
                    fileUuid,
                    "GENERATING",
                    "Separando o que vira questão boa.",
                    null);
            case EventTypes.STUDY_PROBLEMS_GENERATED -> new IngestionStatusResponse(
                    fileUuid,
                    "READY",
                    "Prova pronta para praticar.",
                    null);
            case EventTypes.PDF_INGESTION_FAILED -> new IngestionStatusResponse(
                    fileUuid,
                    "FAILED",
                    "O Ratto travou nessa leitura.",
                    reason);
            default -> new IngestionStatusResponse(
                    fileUuid,
                    "QUEUED",
                    "O Ratto está pegando seus materiais.",
                    null);
        };
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

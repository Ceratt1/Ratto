CREATE TABLE event_ledger (
    ledger_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_name VARCHAR(255) NOT NULL,
    partition_number INTEGER NOT NULL,
    topic_offset BIGINT NOT NULL,
    message_key VARCHAR(255),
    event_id UUID GENERATED ALWAYS AS (
        NULLIF(payload -> 'metadata' ->> 'eventId', '')::UUID
    ) STORED,
    correlation_id UUID GENERATED ALWAYS AS (
        NULLIF(payload -> 'metadata' ->> 'correlationId', '')::UUID
    ) STORED,
    causation_id UUID GENERATED ALWAYS AS (
        NULLIF(payload -> 'metadata' ->> 'causationId', '')::UUID
    ) STORED,
    event_type VARCHAR(255) GENERATED ALWAYS AS (
        payload -> 'metadata' ->> 'eventType'
    ) STORED,
    source_service VARCHAR(255) GENERATED ALWAYS AS (
        payload -> 'metadata' ->> 'sourceService'
    ) STORED,
    schema_version VARCHAR(50) GENERATED ALWAYS AS (
        payload -> 'metadata' ->> 'schemaVersion'
    ) STORED,
    uuid_user UUID GENERATED ALWAYS AS (
        NULLIF(payload ->> 'uuidUser', '')::UUID
    ) STORED,
    uuid_request UUID GENERATED ALWAYS AS (
        NULLIF(payload ->> 'uuidRequest', '')::UUID
    ) STORED,
    file_uuid UUID GENERATED ALWAYS AS (
        NULLIF(payload ->> 'fileUuid', '')::UUID
    ) STORED,
    kafka_timestamp TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload JSONB NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    CONSTRAINT uk_event_ledger_position UNIQUE (topic_name, partition_number, topic_offset)
);

CREATE INDEX idx_event_ledger_correlation_id ON event_ledger (correlation_id);
CREATE INDEX idx_event_ledger_uuid_request ON event_ledger (uuid_request);
CREATE INDEX idx_event_ledger_file_uuid ON event_ledger (file_uuid);
CREATE INDEX idx_event_ledger_event_type ON event_ledger (event_type);
CREATE INDEX idx_event_ledger_recorded_at ON event_ledger (recorded_at);

CREATE FUNCTION reject_event_ledger_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'event_ledger is append-only; % is not allowed', TG_OP;
END;
$$;

CREATE TRIGGER event_ledger_reject_update
BEFORE UPDATE ON event_ledger
FOR EACH ROW EXECUTE FUNCTION reject_event_ledger_mutation();

CREATE TRIGGER event_ledger_reject_delete
BEFORE DELETE ON event_ledger
FOR EACH ROW EXECUTE FUNCTION reject_event_ledger_mutation();

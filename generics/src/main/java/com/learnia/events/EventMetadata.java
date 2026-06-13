package com.learnia.events;

import java.util.UUID;

public record EventMetadata(
        UUID eventId,
        UUID correlationId,
        UUID causationId,
        String eventType,
        String sourceService,
        String schemaVersion,
        String occurredAt) {
}

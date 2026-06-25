package com.learnia.ledger.api.dto;

import java.util.UUID;

public record IngestionStatusResponse(
        UUID fileUuid,
        String status,
        String message,
        String failedReason) {
}

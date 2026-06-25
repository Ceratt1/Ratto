package com.learnia.ledger.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.learnia.ledger.api.dto.IngestionStatusResponse;
import com.learnia.ledger.persistence.EventLedgerRepository;

@RestController
@RequestMapping("/internal/v1/ingestion-status")
public class IngestionStatusController {

    private final EventLedgerRepository repository;

    public IngestionStatusController(EventLedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{fileUuid}")
    public IngestionStatusResponse getStatus(
            @PathVariable UUID fileUuid,
            @RequestParam UUID uuidUser) {
        return repository.findIngestionStatus(uuidUser, fileUuid);
    }
}

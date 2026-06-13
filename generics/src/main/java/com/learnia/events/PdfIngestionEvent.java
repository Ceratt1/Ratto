package com.learnia.events;

import java.util.UUID;

public interface PdfIngestionEvent {

    EventMetadata metadata();

    UUID uuidUser();

    UUID uuidRequest();

    UUID fileUuid();
}

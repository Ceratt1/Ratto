package com.learnia.events;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class EventIdFactory {

    private EventIdFactory() {
    }

    public static UUID forFile(String eventType, UUID fileUuid) {
        String eventIdentity = eventType + ":" + fileUuid;
        return UUID.nameUUIDFromBytes(eventIdentity.getBytes(StandardCharsets.UTF_8));
    }
}

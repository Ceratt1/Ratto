package com.learnia.producer.models.dto;

import java.util.List;
import java.util.UUID;

public record PreparedUploadDto(
        UUID uuidRequest,
        UUID uuidUser,
        List<PreparedFileUploadDto> files) {
}

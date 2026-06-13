package com.learnia.producer.models.dto;

import java.util.UUID;

public record PreparedFileUploadDto(
        UUID fileUuid,
        String fileName,
        String s3Path,
        String uploadUrl,
        String contentType) {
}

package com.learnia.producer.models.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfirmFileUploadRequest(
        @NotNull UUID fileUuid,
        @NotBlank
        @Pattern(regexp = "(?i)^[^/\\\\]+\\.pdf$", message = "fileName must be a PDF without path separators")
        String fileName,
        @NotBlank String s3Path) {
}

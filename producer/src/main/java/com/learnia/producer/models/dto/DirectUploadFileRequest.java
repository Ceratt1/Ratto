package com.learnia.producer.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record DirectUploadFileRequest(
        @NotBlank
        @Pattern(regexp = "(?i)^[^/\\\\]+\\.pdf$", message = "fileName must be a PDF without path separators")
        String fileName,
        @Pattern(regexp = "(?i)^application/pdf$", message = "contentType must be application/pdf")
        String contentType,
        @Min(value = 1, message = "sizeBytes must be positive")
        @Max(value = 31_457_280, message = "PDF must have at most 30 MB")
        long sizeBytes) {

    public String resolvedContentType() {
        return contentType == null || contentType.isBlank() ? "application/pdf" : contentType;
    }
}

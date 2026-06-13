package com.learnia.producer.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DirectUploadFileRequest(
        @NotBlank
        @Pattern(regexp = "(?i)^[^/\\\\]+\\.pdf$", message = "fileName must be a PDF without path separators")
        String fileName,
        @Pattern(regexp = "(?i)^application/pdf$", message = "contentType must be application/pdf")
        String contentType) {

    public String resolvedContentType() {
        return contentType == null || contentType.isBlank() ? "application/pdf" : contentType;
    }
}

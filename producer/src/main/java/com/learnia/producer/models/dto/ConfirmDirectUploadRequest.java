package com.learnia.producer.models.dto;

import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmDirectUploadRequest(
        UUID workspaceId,
        @Length(max = 200) String description,
        @NotBlank
        @Pattern(regexp = "^(en|pt-BR|es)$", message = "studyLanguage must be one of en, pt-BR, es")
        String studyLanguage,
        @NotEmpty
        @Size(min = 1, max = 1, message = "Confirm exactly one PDF")
        List<@Valid ConfirmFileUploadRequest> files) {
}

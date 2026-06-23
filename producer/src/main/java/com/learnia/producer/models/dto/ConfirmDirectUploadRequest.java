package com.learnia.producer.models.dto;

import java.util.List;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ConfirmDirectUploadRequest(
        @Length(max = 200) String description,
        @NotEmpty
        @Size(min = 1, max = 1, message = "Confirm exactly one PDF")
        List<@Valid ConfirmFileUploadRequest> files) {
}

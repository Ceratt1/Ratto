package com.learnia.producer.models.dto;

import java.util.List;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record DirectUploadRequest(
        @Length(max = 200) String description,
        @NotEmpty
        @Size(min = 1, max = 1, message = "Send exactly one PDF")
        List<@Valid DirectUploadFileRequest> files) {
}

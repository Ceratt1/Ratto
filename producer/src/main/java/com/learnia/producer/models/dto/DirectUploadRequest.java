package com.learnia.producer.models.dto;

import java.util.List;
import java.util.UUID;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record DirectUploadRequest(
        @NotNull UUID uuidUser,
        @Length(max = 200) String description,
        @NotEmpty List<@Valid DirectUploadFileRequest> files) {
}

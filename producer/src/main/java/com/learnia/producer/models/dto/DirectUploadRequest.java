package com.learnia.producer.models.dto;

import java.util.List;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record DirectUploadRequest(
        @Length(max = 200) String description,
        @NotEmpty List<@Valid DirectUploadFileRequest> files) {
}

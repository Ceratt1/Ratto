package com.learnia.core.study.api.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class WorkspaceDtos {

    private WorkspaceDtos() {
    }

    public record WorkspaceRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description) {
    }

    public record WorkspaceResponse(
            UUID id,
            String name,
            String description,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }
}

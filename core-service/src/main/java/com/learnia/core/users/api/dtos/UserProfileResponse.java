package com.learnia.core.users.api.dtos;

import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String firstName, String lastName) {
}

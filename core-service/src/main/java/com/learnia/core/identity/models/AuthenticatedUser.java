package com.learnia.core.identity.models;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String firstName, String lastName) {
}

package com.learnia.core.users.models;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.learnia.core.identity.models.AuthenticatedUser;

public record UserProfile(
        UUID id,
        String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserProfile from(AuthenticatedUser user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new UserProfile(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                now,
                now);
    }

    public UserProfile synchronize(AuthenticatedUser user) {
        return new UserProfile(
                id,
                user.email(),
                user.firstName(),
                user.lastName(),
                createdAt,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}

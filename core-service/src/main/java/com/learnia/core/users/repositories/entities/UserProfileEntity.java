package com.learnia.core.users.repositories.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.learnia.core.users.models.UserProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profile")
public class UserProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserProfileEntity() {
    }

    public static UserProfileEntity from(UserProfile profile) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.id = profile.id();
        entity.email = profile.email();
        entity.firstName = profile.firstName();
        entity.lastName = profile.lastName();
        entity.createdAt = profile.createdAt();
        entity.updatedAt = profile.updatedAt();
        return entity;
    }

    public UserProfile toModel() {
        return new UserProfile(id, email, firstName, lastName, createdAt, updatedAt);
    }
}

package com.learnia.core.users.repositories;

import java.util.Optional;
import java.util.UUID;

import com.learnia.core.users.models.UserProfile;

public interface UserProfileRepository {

    Optional<UserProfile> findById(UUID id);

    UserProfile save(UserProfile profile);
}

package com.learnia.core.users.repositories.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.learnia.core.users.models.UserProfile;
import com.learnia.core.users.repositories.UserProfileRepository;
import com.learnia.core.users.repositories.entities.UserProfileEntity;

@Repository
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final JpaUserProfileRepository repository;

    public UserProfileRepositoryImpl(JpaUserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return repository.findById(id).map(UserProfileEntity::toModel);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        return repository.save(UserProfileEntity.from(profile)).toModel();
    }
}

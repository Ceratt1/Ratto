package com.learnia.core.users.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.learnia.core.identity.models.AuthenticatedUser;
import com.learnia.core.users.models.UserProfile;
import com.learnia.core.users.repositories.UserProfileRepository;
import com.learnia.core.users.services.UserProfileService;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileServiceImpl(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UserProfile synchronize(AuthenticatedUser user) {
        UserProfile profile = repository.findById(user.id())
                .map(existing -> existing.synchronize(user))
                .orElseGet(() -> UserProfile.from(user));
        return repository.save(profile);
    }
}

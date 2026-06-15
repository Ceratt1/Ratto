package com.learnia.core.users.repositories.impl;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.learnia.core.users.repositories.entities.UserProfileEntity;

interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
}

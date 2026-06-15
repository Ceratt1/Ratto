package com.learnia.core.identity.services;

import org.springframework.security.oauth2.jwt.Jwt;

import com.learnia.core.identity.models.AuthenticatedUser;

public interface AuthenticatedUserService {

    AuthenticatedUser from(Jwt jwt);
}

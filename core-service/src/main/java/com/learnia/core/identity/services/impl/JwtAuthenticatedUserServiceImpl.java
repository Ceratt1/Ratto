package com.learnia.core.identity.services.impl;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.learnia.core.identity.models.AuthenticatedUser;
import com.learnia.core.identity.services.AuthenticatedUserService;

@Service
public class JwtAuthenticatedUserServiceImpl
        implements AuthenticatedUserService, Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthenticatedUser user = from(jwt);
        return new JwtAuthenticationToken(jwt, java.util.List.of(), user.id().toString());
    }

    @Override
    public AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(
                parseUuid(jwt.getSubject()),
                requiredClaim(jwt, "email"),
                requiredClaim(jwt, "given_name"),
                requiredClaim(jwt, "family_name"));
    }

    private UUID parseUuid(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("JWT sub must be a UUID", exception);
        }
    }

    private String requiredClaim(Jwt jwt, String claim) {
        String value = jwt.getClaimAsString(claim);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("JWT claim is required: " + claim);
        }
        return value;
    }
}

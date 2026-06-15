package com.learnia.core.users.api.controllers;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learnia.core.identity.services.AuthenticatedUserService;
import com.learnia.core.users.api.dtos.UserProfileResponse;
import com.learnia.core.users.models.UserProfile;
import com.learnia.core.users.services.UserProfileService;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserProfileService userProfileService;

    public UserProfileController(
            AuthenticatedUserService authenticatedUserService,
            UserProfileService userProfileService) {
        this.authenticatedUserService = authenticatedUserService;
        this.userProfileService = userProfileService;
    }

    @PutMapping("/me")
    public UserProfileResponse synchronize(@AuthenticationPrincipal Jwt jwt) {
        UserProfile profile = userProfileService.synchronize(authenticatedUserService.from(jwt));
        return new UserProfileResponse(
                profile.id(),
                profile.email(),
                profile.firstName(),
                profile.lastName());
    }
}

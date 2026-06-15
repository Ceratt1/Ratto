package com.learnia.core.users.services;

import com.learnia.core.identity.models.AuthenticatedUser;
import com.learnia.core.users.models.UserProfile;

public interface UserProfileService {

    UserProfile synchronize(AuthenticatedUser user);
}

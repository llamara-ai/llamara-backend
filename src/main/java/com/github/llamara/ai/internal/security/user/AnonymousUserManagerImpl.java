package com.github.llamara.ai.internal.security.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import com.github.llamara.ai.internal.security.Users;

/**
 * {@link UserManager} implementation for handling anonymous users.
 *
 * <p>Anonymous users are not stored in the database.
 *
 * @author Florian Hotze - Initial contribution
 */
@Typed(AnonymousUserManagerImpl.class)
@ApplicationScoped
public class AnonymousUserManagerImpl implements UserManager {
    @Override
    public boolean register() {
        return true;
    }

    @Override
    public void enforceRegistered() throws UserNotRegisteredException {
        // do nothing
    }

    @Override
    public void delete() {
        // Do nothing here
    }

    @Override
    public User getUser() {
        return Users.ANY;
    }
}
